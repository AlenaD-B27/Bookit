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
import io.restassured.response.ValidatableResponse;
import org.junit.Assert;

import java.util.Map;
import java.util.Objects;

import static io.restassured.RestAssured.*;

public class ApiStepDefs {

    String token;
    Response response;
    String email;

    Map<String, Object> postInfo;

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

    @When("I send POST request {string} endpoint with following information")
    public void i_send_post_request_endpoint_with_following_information(String endPoint, Map<String,String> userInfo) {
        response = given().accept(ContentType.JSON)
                .header("Authorization", token)
                .queryParams(userInfo)
                .when().post(Environment.BASE_URL + endPoint).prettyPeek();
    }
    @Then("I delete previously added student")
    public void i_delete_previously_added_student() {
        int idToDelete = response.path("entryiId");
        given().header("Authorization", token)
                .pathParam("id", idToDelete)
                .delete(Environment.BASE_URL + "/api/students/{id}")
                .then().statusCode(204);
    }



    @When("Users sends POST request to {string} with following info:")
    public void users_sends_post_request_to_with_following_info(String endPoint, Map<String, Object> teamInfo) {

        response = given().accept(ContentType.JSON).header("Authorization", token)
                .queryParams(teamInfo)
                .post(Environment.BASE_URL + endPoint).prettyPeek();

        Assert.assertEquals(201, response.getStatusCode());

        postInfo = teamInfo;

    }
    @Then("Database should persist same team info")
    public void database_should_persist_same_team_info() {



        DB_Util.runQuery("select name, batch_number, c.location from team t left join batch b\n" +
                "    on t.batch_number = b.number\n" +
                "left join campus c\n" +
                "    on t.campus_id = c.id\n" +
                "where t.id = " + response.jsonPath().getInt("entryiId") + ";");

        Map<String, String> expectedTeamInfoFromDB = DB_Util.getRowMap(1);

        Assert.assertEquals(expectedTeamInfoFromDB.get("name"),postInfo.get("team-name"));
        Assert.assertEquals(expectedTeamInfoFromDB.get("batch_number"),postInfo.get("batch-number"));
        Assert.assertEquals(expectedTeamInfoFromDB.get("location"),postInfo.get("campus-location"));

    }
    @Then("User deletes previously created team")
    public void user_deletes_previously_created_team() {
        given().accept(ContentType.JSON).header("Authorization", token)
                .pathParam("id",response.jsonPath().getInt("entryiId"))
                .when().delete(Environment.BASE_URL + "/api/teams/{id}")
                .then().statusCode(200);
    }

}
