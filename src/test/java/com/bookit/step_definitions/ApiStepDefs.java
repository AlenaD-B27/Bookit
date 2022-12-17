package com.bookit.step_definitions;

import com.bookit.pages.SelfPage;
import com.bookit.pages.SignInPage;
import com.bookit.utilities.BookitUtils;
import com.bookit.utilities.ConfigurationReader;
import com.bookit.utilities.DB_Util;
import com.bookit.utilities.Environment;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.junit.Assert;

import java.util.Map;

import static io.restassured.RestAssured.*;

public class ApiStepDefs {

    String token;
    Response response;
    String email;

    @Given("I logged Bookit api as a {string}")
    public void i_logged_bookit_api_as_a(String role) {
        token = BookitUtils.generateTokenByRole(role);

        Map<String, String> credentials = BookitUtils.returnCredentials(role);
        email = credentials.get("email");

    }
    @When("I sent get request to {string} endpoint")
    public void i_sent_get_request_to_endpoint(String endpoint) {
        response = given().accept(ContentType.JSON)
                .header("Authorization", token)
                .when().get(Environment.BASE_URL + endpoint);
    }
    @Then("status code should be {int}")
    public void status_code_should_be(int statusCode) {
        Assert.assertEquals(statusCode, response.getStatusCode());
    }
    @Then("content type is {string}")
    public void content_type_is(String contentType) {
        Assert.assertEquals(contentType,response.contentType());
    }
    @Then("role is {string}")
    public void role_is(String role) {
        Assert.assertEquals(role,response.jsonPath().getString("role"));
    }

    @Then("the information about current user from api and database should match")
    public void the_information_about_current_user_from_api_and_database_should_match() {
       // Get data from API

        JsonPath jsonPath = response.jsonPath();
        String actualLastName = jsonPath.getString("lastName");
        String actualFirstName = jsonPath.getString("firstName");
        String actualRole = jsonPath.getString("role");

        // Get data from DB

        String query = "select firstname,lastname,role from users\n" +
                "where email='" + email + "'";

        DB_Util.runQuery(query);
        Map<String, String> dbMap = DB_Util.getRowMap(1);

        // Assert

        Assert.assertEquals(dbMap.get("firstname"),actualFirstName);
        Assert.assertEquals(dbMap.get("lastname"),actualLastName);
        Assert.assertEquals(dbMap.get("role"),actualRole);
    }


    @Then("UI,API and Database user information must be match")
    public void ui_api_and_database_user_information_must_be_match() {
        // Get data from API

        JsonPath jsonPath = response.jsonPath();
        String actualLastName = jsonPath.getString("lastName");
        String actualFirstName = jsonPath.getString("firstName");
        String actualRole = jsonPath.getString("role");

        // Get data from DB

        String query = "select firstname,lastname,role from users\n" +
                "where email='" + email + "'";

        DB_Util.runQuery(query);
        Map<String, String> dbMap = DB_Util.getRowMap(1);

        String expectedFirstname = dbMap.get("firstname");
        String expectedLastname = dbMap.get("lastname");
        String expectedRole = dbMap.get("role");

        // Assert API vs DB

        Assert.assertEquals(expectedFirstname,actualFirstName);
        Assert.assertEquals(expectedLastname,actualLastName);
        Assert.assertEquals(expectedRole,actualRole);

        // Get data from UI

        String actualNameFromUI = new SelfPage().name.getText();
        String actualRoleFromUI = new SelfPage().role.getText();

        // Assert UI vs DB

        String expectedNameFromDB = expectedFirstname + " " + expectedLastname;
        Assert.assertEquals(expectedNameFromDB,actualNameFromUI);
        Assert.assertEquals(expectedRole,actualRoleFromUI);


        // Assert UI vs API

        String actualNameFromAPI = actualFirstName + " " + actualLastName;
        Assert.assertEquals(actualNameFromAPI,actualNameFromUI);
        Assert.assertEquals(actualRole,actualRoleFromUI);
    }

}
