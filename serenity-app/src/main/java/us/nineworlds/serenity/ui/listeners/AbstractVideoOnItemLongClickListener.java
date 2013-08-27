/**
 * The MIT License (MIT)
 * Copyright (c) 2012 David Carver
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS
 * OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF
 * OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package us.nineworlds.serenity.ui.listeners;

import java.util.ArrayList;
import java.util.List;

import com.castillo.dd.DSInterface;
import com.castillo.dd.PendingDownload;
import com.google.analytics.tracking.android.Log;

import us.nineworlds.serenity.MainActivity;
import us.nineworlds.serenity.R;
import us.nineworlds.serenity.SerenityApplication;
import us.nineworlds.serenity.core.SerenityConstants;
import us.nineworlds.serenity.core.model.VideoContentInfo;
import us.nineworlds.serenity.core.services.UnWatchVideoAsyncTask;
import us.nineworlds.serenity.core.services.WatchedVideoAsyncTask;
import us.nineworlds.serenity.ui.dialogs.DirectoryChooserDialog;
import us.nineworlds.serenity.ui.util.ImageInfographicUtils;
import us.nineworlds.serenity.ui.util.VideoPlayerIntentUtils;
import us.nineworlds.serenity.ui.video.player.SerenitySurfaceViewVideoActivity;
import us.nineworlds.serenity.ui.views.SerenityPosterImageView;
import us.nineworlds.serenity.widgets.SerenityAdapterView;
import us.nineworlds.serenity.widgets.SerenityGallery;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.preference.PreferenceManager;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

/**
 * A listener that handles long press for video content. Includes displaying a
 * dialog for toggling watched and unwatched status as well for possibly playing
 * on the TV.
 * 
 * @author dcarver
 * 
 */
public class AbstractVideoOnItemLongClickListener {

	protected Dialog dialog;
	protected Activity context;
	protected VideoContentInfo info;
	protected SerenityPosterImageView vciv;

	public boolean onItemLongClick(SerenityAdapterView<?> av, View v,
			int position, long arg3) {

		// Google TV is sending back different results than Nexus 7
		// So we try to handle the different results.

		if (v == null) {
			SerenityGallery g = (SerenityGallery) av;
			vciv = (SerenityPosterImageView) g.getSelectedView();
		} else {
			if (v instanceof SerenityPosterImageView) {
				vciv = (SerenityPosterImageView) v;
			} else {
				SerenityGallery g = (SerenityGallery) v;
				vciv = (SerenityPosterImageView) g.getSelectedView();
			}
		}

		return onItemLongClick();
	}

	/**
	 * @return
	 */
	protected boolean onItemLongClick() {
		info = vciv.getPosterInfo();
		context = (Activity) vciv.getContext();

		dialog = new Dialog(context);
		AlertDialog.Builder builder = new AlertDialog.Builder(
				new ContextThemeWrapper(context,
						android.R.style.Theme_Holo));
		builder.setTitle(context.getString(R.string.video_options));

		ListView modeList = new ListView(context);
		ArrayList<String> options = new ArrayList<String>();
		options.add(context.getString(R.string.toggle_watched_status));
		options.add(context.getString(R.string.download_video_to_device));
		options.add("Add to Video Playback Queue");
		options.add("Play all Videos in Queue");
		if (!SerenityApplication.isGoogleTV(context)) {
			options.add(context.getString(R.string.play_on_tv));
		}

		ArrayAdapter<String> modeAdapter = new ArrayAdapter<String>(context,
				android.R.layout.simple_list_item_1, android.R.id.text1,
				options);

		modeList.setAdapter(modeAdapter);
		modeList.setOnItemClickListener(new DialogOnItemSelected());

		builder.setView(modeList);
		dialog = builder.create();
		dialog.show();

		return true;
	}

	protected boolean hasAbleRemote(Context context) {

		final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
		mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
		final List<ResolveInfo> pkgAppsList = context.getPackageManager()
				.queryIntentActivities(mainIntent, 0);

		for (ResolveInfo resolveInfo : pkgAppsList) {
			String packageName = resolveInfo.activityInfo.packageName;
			if (packageName.contains("entertailion.android.remote")) {
				return true;
			}
		}

		return false;
	}
	
	protected void performWatchedToggle() {
		if (info.getViewCount() > 0) {
			new UnWatchVideoAsyncTask().execute(info.id());
			ImageInfographicUtils.setUnwatched(vciv, context);
		} else {
			new WatchedVideoAsyncTask().execute(info.id());
			ImageInfographicUtils.setWatchedCount(vciv, context);
		}
	}
	
	protected void performGoogleTVSecondScreen() {
		if (hasAbleRemote(context)) {
			Intent sharingIntent = new Intent();
			sharingIntent.setClassName(
					"com.entertailion.android.remote",
					"com.entertailion.android.remote.MainActivity");
			sharingIntent.setAction("android.intent.action.SEND");
			sharingIntent.putExtra(Intent.EXTRA_TEXT, vciv
					.getPosterInfo().getDirectPlayUrl());

			context.startActivity(sharingIntent);
		}
	}
	
	protected void performAddToQueue() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		boolean extplayer = prefs.getBoolean("external_player", false);
		boolean extplayerVideoQueue = prefs.getBoolean("external_player_continuous_playback", false);
		if (extplayer && !extplayerVideoQueue) {
			Toast.makeText(context, "External player video queue support has not been enabled.", Toast.LENGTH_LONG).show();
			return;
		}

		SerenityApplication.getVideoPlaybackQueue().add(info);
	}

	protected class DialogOnItemSelected implements OnItemClickListener {

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * android.widget.AdapterView.OnItemClickListener#onItemClick(android
		 * .widget.AdapterView, android.view.View, int, long)
		 */
		@Override
		public void onItemClick(android.widget.AdapterView<?> arg0, View v,
				int position, long arg3) {

			switch (position) {
			case 0:
				performWatchedToggle();
				break;
			case 1:
				startDownload();
				break;
			case 2:
				performAddToQueue();
				break;
			case 3:
				if (!SerenityApplication.getVideoPlaybackQueue().isEmpty()) {
					SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
					boolean extplayer = prefs.getBoolean("external_player", false);
					boolean mxplayer = prefs.getBoolean("mxplayer_plex_offset", false);
					boolean extplayerVideoQueue = prefs.getBoolean("external_player_continuous_playback", false);
	
					
					if (extplayer) {
						if (extplayerVideoQueue) {
							VideoContentInfo videoContent = SerenityApplication.getVideoPlaybackQueue().poll();
							VideoPlayerIntentUtils.launchExternalPlayer(videoContent, mxplayer, context);
						} else {
							Toast.makeText(context, "External player video queue support has not been enabled.", Toast.LENGTH_LONG).show();
						}
					} else {
						Intent vpIntent = new Intent(context,
								SerenitySurfaceViewVideoActivity.class);
						context.startActivityForResult(vpIntent, SerenityConstants.EXIT_PLAYBACK_IMMEDIATELY);
					}
				}
				break;
			default:
				performGoogleTVSecondScreen();
			}
			dialog.dismiss();
		}

	}

	protected void startDownload() {
		directoryChooser();
	}

	protected void startDownload(String destination) {

		List<PendingDownload> pendingDownloads = SerenityApplication
				.getPendingDownloads();
		PendingDownload pendingDownload = new PendingDownload();
		String filename = info.getTitle() + "." + info.getContainer();
		pendingDownload
				.setFilename(filename);
		pendingDownload.setUrl(info.getDirectPlayUrl());

		pendingDownloads.add(pendingDownload);
		int pos = pendingDownloads.size() - 1;

		try {
			DSInterface downloadService = MainActivity.getDsInterface();
			downloadService.addFileDownloadlist(info.getDirectPlayUrl(), destination, filename, pos);
			Toast.makeText(
					context,
					context.getString(R.string.starting_download_of_)
							+ info.getTitle(), Toast.LENGTH_LONG).show();
		} catch (Exception ex) {
			Log.e("Unable to download " + info.getTitle() + "."
					+ info.getContainer());
		}
	}

	protected void directoryChooser() {
		// Create DirectoryChooserDialog and register a callback
		DirectoryChooserDialog directoryChooserDialog = new DirectoryChooserDialog(
				context, new DirectoryChooserDialog.ChosenDirectoryListener() {
					@Override
					public void onChosenDir(String chosenDir) {
						Toast.makeText(
								context,
								context.getString(R.string.chosen_directory_)
										+ chosenDir, Toast.LENGTH_LONG).show();
						startDownload(chosenDir);
					}
				});
		directoryChooserDialog.setNewFolderEnabled(true);
		directoryChooserDialog.chooseDirectory("");
	}

}
