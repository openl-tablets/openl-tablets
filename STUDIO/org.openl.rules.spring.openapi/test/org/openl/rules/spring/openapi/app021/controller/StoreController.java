package org.openl.rules.spring.openapi.app021.controller;

import java.util.Map;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

import org.openl.rules.spring.openapi.app020.model.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "store", description = "Access to Petstore orders")
@RequestMapping("/store")
public class StoreController {

    /**
     * DELETE /store/order/{orderId} : Delete purchase order by ID For valid response try integer IDs with positive
     * integer value. Negative or non-integer values will generate API errors
     *
     * @param orderId ID of the order that needs to be deleted (required)
     * @return Invalid ID supplied (status code 400) or Order not found (status code 404)
     */
    @Operation(summary = "Delete purchase order by ID", description = "For valid response try integer IDs with positive integer value. Negative or non-integer values will generate API errors", responses = {
            @ApiResponse(responseCode = "400", description = "Invalid ID supplied"),
            @ApiResponse(responseCode = "404", description = "Order not found") })
    @DeleteMapping(value = "/order/{orderId}")
    public ResponseEntity<Void> deleteOrder(
            @Parameter(description = "ID of the order that needs to be deleted") @Min(1) @PathVariable("orderId") Long orderId) {
        return ResponseEntity.ok().build();
    }

    /**
     * GET /inventory : Returns pet inventories by status Returns a map of status codes to quantities
     *
     * @return successful operation (status code 200)
     */
    @Operation(summary = "Returns pet inventories by status", description = "Returns a map of status codes to quantities")
    @ApiResponse(responseCode = "200", description = "successful operation")
    @GetMapping(value = "/inventory", produces = { "application/json" })
    public ResponseEntity<Map<String, Integer>> getInventory() {
        return ResponseEntity.ok().build();
    }

    /**
     * GET /order/{orderId} : Find purchase order by ID For valid response try integer IDs with value &gt;&#x3D; 1 and
     * &lt;&#x3D; 10. Other values will generated exceptions
     *
     * @param orderId ID of pet that needs to be fetched (required)
     * @return successful operation (status code 200) or Invalid ID supplied (status code 400) or Order not found
     *         (status code 404)
     */
    @Operation(summary = "Find purchase order by ID", description = "For valid response try integer IDs with value >= 1 and <= 10. Other values will generated exceptions", responses = {
            @ApiResponse(responseCode = "200", description = "successful operation"),
            @ApiResponse(responseCode = "400", description = "Invalid ID supplied"),
            @ApiResponse(responseCode = "404", description = "Order not found") })
    @GetMapping(value = "/order/{orderId}", produces = { "application/json", "application/xml" })
    public ResponseEntity<Order> getOrderById(
            @Parameter(description = "ID of pet that needs to be fetched") @Min(1) @Max(10) @PathVariable("orderId") Long orderId) {
        return ResponseEntity.ok().build();
    }

    /**
     * POST /order : Place an order for a pet
     *
     * @param order order placed for purchasing the pet (required)
     * @return successful operation (status code 200) or Invalid Order (status code 400)
     */
    @Operation(summary = "Place an order for a pet", tags = { "store" }, responses = {
            @ApiResponse(responseCode = "200", description = "successful operation"),
            @ApiResponse(responseCode = "400", description = "Invalid Order") })
    @PostMapping(value = "/order", produces = { "application/json", "application/xml" }, consumes = {
            "application/json" })
    public ResponseEntity<Order> placeOrder(
            @Parameter(description = "order placed for purchasing the pet") @RequestBody Order order) {
        return ResponseEntity.ok().build();
    }

}
