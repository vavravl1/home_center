package controllers

import org.scalatest.{Matchers, WordSpec}
import play.api.mvc.Cookies
import play.api.test.FakeRequest
import play.api.test.Helpers._

/**
  *
  */
class SignInControllerTest extends WordSpec with Matchers with IntegrationTest {

  "SignInController" when {
    "there is no logged user" should {
      "has correct log in page" in {
        val request = FakeRequest(GET, "/signIn").withHeaders()
        val signInPage = route(app, request).get

        status(signInPage) shouldBe OK
        contentType(signInPage) shouldBe Some("text/html")
        //More assertions here are currently difficult as the page is rendered by javascript
      }
      "valid user can sign in" in {
        val request = FakeRequest(POST, "/signIn").withFormUrlEncodedBody(
          ("email","vlvavra@cisco.com"),
          ("password","123"),
          ("rememberMe", "true")
        )
        val signInPage = route(app, request).get

        status(signInPage) shouldBe SEE_OTHER
        val cookie:Cookies = cookies(signInPage)
        cookie should have size(1)
        cookie.get("id").isDefined shouldBe true
        cookie.get("id").get.maxAge shouldBe Some(43200) //12 hours
        cookie.get("id").get.value.length should be > 600
      }

      "invalid user must not sign in" in {
        val request = FakeRequest(POST, "/signIn").withFormUrlEncodedBody(
          ("email","vlvavra@cisco.com"),
          ("password","XXX"),
          ("rememberMe", "true")
        )
        val signInPage = route(app, request).get

        status(signInPage) shouldBe UNAUTHORIZED
        cookies(signInPage) should be (empty)
      }

      "logged user has visible email on main page" in {
        val singInRequest = FakeRequest(POST, "/signIn").withFormUrlEncodedBody(
          ("email","vlvavra@cisco.com"),
          ("password","123"),
          ("rememberMe", "true")
        )
        val signInPage = route(app, singInRequest).get

        val dataRequest = FakeRequest(GET, "/data").withCookies(cookies(signInPage).get("id").get)
        val dataPage = route(app, dataRequest).get

        status(dataPage) shouldBe OK
        contentType(dataPage) shouldBe Some("text/html")
        contentAsString(dataPage) should include ("vlvavra@cisco.com")

      }
    }
  }
}
