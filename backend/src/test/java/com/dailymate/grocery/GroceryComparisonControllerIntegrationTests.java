package com.dailymate.grocery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dailymate.auth.dto.request.RegisterRequest;
import com.dailymate.grocery.dto.request.GroceryItemRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GroceryComparisonControllerIntegrationTests {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String registerAndGetToken(String email) throws Exception {
        String tokenBody = mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest(email, "StrongPass123!", "Grocery", "Shopper"))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(tokenBody).get("accessToken").asText();
    }

    @Test
    void anonymousCanReadPublicGroceryFeedButCannotAccessPrivateEndpointsOrMutate() throws Exception {
        // Public feed -> 200 OK
        mvc.perform(get("/api/v1/grocery/items"))
                .andExpect(status().isOk());

        // My items -> 401 Unauthorized
        mvc.perform(get("/api/v1/grocery/my-items"))
                .andExpect(status().isUnauthorized());

        GroceryItemRequest req = new GroceryItemRequest(
                "Basmati Rice", "Grains & Pulses", "Fresh Mart", new BigDecimal("120.00"), "1 kg", "Downtown");

        // Anonymous POST -> 401 Unauthorized
        mvc.perform(post("/api/v1/grocery/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());

        // Anonymous PATCH -> 401 Unauthorized
        mvc.perform(patch("/api/v1/grocery/items/fake-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());

        // Anonymous DELETE -> 401 Unauthorized
        mvc.perform(delete("/api/v1/grocery/items/fake-id"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void publicFeedSupportsSearchCategoryAndStoreFilters() throws Exception {
        String token = registerAndGetToken("filter-shopper@example.com");

        GroceryItemRequest item1 = new GroceryItemRequest(
                "Amul Taaza Milk", "Dairy & Eggs", "D-Mart", new BigDecimal("66.00"), "1 L", "Kothrud");
        GroceryItemRequest item2 = new GroceryItemRequest(
                "Fortune Sunlite Sunflower Oil", "Grains & Pulses", "Reliance Fresh", new BigDecimal("145.00"), "1 L", "Aundh");

        mvc.perform(post("/api/v1/grocery/items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(item1)))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/v1/grocery/items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(item2)))
                .andExpect(status().isCreated());

        // Search by product name
        String searchRes = mvc.perform(get("/api/v1/grocery/items").param("search", "Milk"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode searchArray = objectMapper.readTree(searchRes);
        assertTrue(searchArray.size() >= 1);
        for (JsonNode node : searchArray) {
            assertTrue(node.get("name").asText().toLowerCase().contains("milk"));
        }

        // Filter by Category
        String catRes = mvc.perform(get("/api/v1/grocery/items").param("category", "Dairy & Eggs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode catArray = objectMapper.readTree(catRes);
        assertTrue(catArray.size() >= 1);
        for (JsonNode node : catArray) {
            assertEquals("Dairy & Eggs", node.get("category").asText());
        }

        // Filter by Store
        String storeRes = mvc.perform(get("/api/v1/grocery/items").param("store", "D-Mart"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode storeArray = objectMapper.readTree(storeRes);
        assertTrue(storeArray.size() >= 1);
        for (JsonNode node : storeArray) {
            assertEquals("D-Mart", node.get("store").asText());
        }
    }

    @Test
    void userCanCreateUpdateAndDeletePersonalGroceryItem() throws Exception {
        String token = registerAndGetToken("crud-shopper@example.com");

        GroceryItemRequest createReq = new GroceryItemRequest(
                "Aashirvaad Whole Wheat Atta", "Grains & Pulses", "Big Bazaar", new BigDecimal("240.00"), "5 kg", "Shivajinagar");

        String postBody = mvc.perform(post("/api/v1/grocery/items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Aashirvaad Whole Wheat Atta"))
                .andExpect(jsonPath("$.unit").value("5 kg"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String itemId = objectMapper.readTree(postBody).get("id").asText();

        // Check in /my-items
        mvc.perform(get("/api/v1/grocery/my-items")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Aashirvaad Whole Wheat Atta"));

        // Update item
        GroceryItemRequest updateReq = new GroceryItemRequest(
                "Aashirvaad Whole Wheat Atta", "Grains & Pulses", "Big Bazaar Super", new BigDecimal("235.00"), "5 kg", "Shivajinagar");

        mvc.perform(patch("/api/v1/grocery/items/{id}", itemId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price").value(235.00))
                .andExpect(jsonPath("$.store").value("Big Bazaar Super"));

        // Delete item -> 204 No Content
        mvc.perform(delete("/api/v1/grocery/items/{id}", itemId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    void userCannotModifyOrDeleteAnotherUsersGroceryItem() throws Exception {
        String tokenUserA = registerAndGetToken("shopper-a@example.com");
        String tokenUserB = registerAndGetToken("shopper-b@example.com");

        GroceryItemRequest req = new GroceryItemRequest(
                "Tata Salt", "Household", "Local Kirana", new BigDecimal("28.00"), "1 kg", "Baner");

        String postBody = mvc.perform(post("/api/v1/grocery/items")
                        .header("Authorization", "Bearer " + tokenUserA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String itemId = objectMapper.readTree(postBody).get("id").asText();

        // User B's my-items does not contain User A's item
        String myItemsB = mvc.perform(get("/api/v1/grocery/my-items")
                        .header("Authorization", "Bearer " + tokenUserB))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode myItemsArray = objectMapper.readTree(myItemsB);
        for (JsonNode item : myItemsArray) {
            assertEquals(false, itemId.equals(item.get("id").asText()));
        }

        // User B cannot PATCH User A's item -> 404
        GroceryItemRequest hijacked = new GroceryItemRequest(
                "Hijacked Salt", "Household", "Nowhere", new BigDecimal("1.00"), "1 kg", "Nowhere");
        mvc.perform(patch("/api/v1/grocery/items/{id}", itemId)
                        .header("Authorization", "Bearer " + tokenUserB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(hijacked)))
                .andExpect(status().isNotFound());

        // User B cannot DELETE User A's item -> 404
        mvc.perform(delete("/api/v1/grocery/items/{id}", itemId)
                        .header("Authorization", "Bearer " + tokenUserB))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsInvalidPayloadWith400() throws Exception {
        String token = registerAndGetToken("invalid-shopper@example.com");

        // Blank name
        GroceryItemRequest blankName = new GroceryItemRequest(
                "", "Household", "Store", new BigDecimal("50.00"), "1 kg", "Location");
        mvc.perform(post("/api/v1/grocery/items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blankName)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists());

        // Blank unit
        GroceryItemRequest blankUnit = new GroceryItemRequest(
                "Item", "Household", "Store", new BigDecimal("50.00"), "", "Location");
        mvc.perform(post("/api/v1/grocery/items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blankUnit)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.unit").exists());

        // Zero price
        GroceryItemRequest zeroPrice = new GroceryItemRequest(
                "Item", "Household", "Store", new BigDecimal("0.00"), "1 kg", "Location");
        mvc.perform(post("/api/v1/grocery/items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(zeroPrice)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.price").exists());
    }
}
