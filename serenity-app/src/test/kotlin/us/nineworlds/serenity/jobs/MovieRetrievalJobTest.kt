package us.nineworlds.serenity.jobs

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.verify
import org.apache.commons.lang3.RandomStringUtils
import org.greenrobot.eventbus.EventBus
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnit
import org.mockito.quality.Strictness.STRICT_STUBS
import org.robolectric.RobolectricTestRunner
import us.nineworlds.serenity.TestingModule
import us.nineworlds.serenity.common.rest.SerenityClient
import us.nineworlds.serenity.events.MovieRetrievalEvent
import us.nineworlds.serenity.test.InjectingTest
import javax.inject.Inject

@RunWith(RobolectricTestRunner::class)
class MovieRetrievalJobTest : InjectingTest() {

  @Rule
  @JvmField
  public val rule = MockitoJUnit.rule().strictness(STRICT_STUBS)

  @Inject
  lateinit var mockClient: SerenityClient

  @Mock
  lateinit var mockEventBus: EventBus

  lateinit var job: MovieRetrievalJob

  private val expectedVideoId: String = RandomStringUtils.randomAlphanumeric(5)
  private val expectedCategory: String = RandomStringUtils.randomAlphabetic(10)

  @Before
  override fun setUp() {
    super.setUp()
    job = MovieRetrievalJob(expectedVideoId, expectedCategory)
    job.eventBus = mockEventBus
  }

  @Test
  fun onRunFetchesCategoriesForTheSpecifiedId() {
    job.onRun()

    verify(mockClient).retrieveItemByIdCategory(expectedVideoId, expectedCategory)
  }

  @Test
  fun onRunFetchesCategoriesAndPostsMainCategoryEvent() {
    job.onRun()

    verify(mockClient).retrieveItemByIdCategory(expectedVideoId, expectedCategory)
    verify(mockEventBus).post(any<MovieRetrievalEvent>())
  }

  override fun installTestModules() {
    scope.installTestModules(TestingModule())
  }

}
