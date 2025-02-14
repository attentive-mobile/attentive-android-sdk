package com.attentive.androidsdk

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VisitorServiceTest {
    private var persistentStorage: PersistentStorage? = null
    private var visitorService: VisitorService? = null

    @Before
    fun setup() {
        persistentStorage =
            PersistentStorage(InstrumentationRegistry.getInstrumentation().targetContext)
        // clear the storage before each test
        persistentStorage!!.deleteAll()
        visitorService = VisitorService(persistentStorage!!)
    }

    @Test
    fun visitorId_firstTime_getsNewVisitorId()
    {
        // Arrange
        // Act
        val visitorId = visitorService!!.visitorId

        // Assert
        verifyVisitorId(visitorId)
    }


    @Test
    fun visitorId_multipleTimes_getsSameVisitorId(){
            // Arrange
            // Act
            val visitorIdFromFirstCall = visitorService!!.visitorId
            val visitorIdFromSecondCall = visitorService!!.visitorId

            // Assert
            Assert.assertEquals(visitorIdFromFirstCall, visitorIdFromSecondCall)
            verifyVisitorId(visitorIdFromFirstCall)
        }

    @Test
    fun createNewVisitorId_afterVisitorIdAlreadyCreated_createsAnotherVisitorId() {
        // Arrange
        // Act
        val oldVisitorId = visitorService!!.visitorId
        val newVisitorId = visitorService!!.createNewVisitorId()

        // Assert
        Assert.assertNotEquals(oldVisitorId, newVisitorId)
        verifyVisitorId(oldVisitorId)
        verifyVisitorId(newVisitorId)
    }

    @Test
    fun createNewVisitorId_noVisitorIdRetrievedYet_createsAValidVisitorId() {
        // Arrange
        // Act
        val visitorId = visitorService!!.createNewVisitorId()

        // Assert
        verifyVisitorId(visitorId)
    }

    private fun verifyVisitorId(visitorId: String) {
        Assert.assertEquals(32, visitorId.length.toLong())
        Assert.assertEquals('4'.code.toLong(), visitorId[12].code.toLong())
    }
}
