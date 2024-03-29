package com.progrespoint.restapihsbc.cotrollers;

import com.progrespoint.restapihsbc.exceptions.ResourceNotFoundException;
import com.progrespoint.restapihsbc.model.Customer;
import com.progrespoint.restapihsbc.services.CustomerService;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.Resources;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

@RestController
@RequestMapping(value = "api/v1/customers", produces = "application/hal+json")
public class CustomerController {

    private final String REL_SELF = "self";
    private final String REL_CUSTOMER_BY_ID = "customersById";
    private final String REL_ALL_CUSTOMERS = "findAllCustomers";

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping
    public ResponseEntity<Resources<Resource<Customer>>> getAllCustomers(){
        Resources<Resource<Customer>> resources = new Resources<>(
                customerService
                        .findAll()
                        .map(this::resource)
                        .collect(Collectors.toList())
        );
        addFindCustomerByIdLink(resources, REL_CUSTOMER_BY_ID);
        addFindAllCustomersLink(resources, REL_SELF);
        return ok(resources);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Resource<Customer>> getCustomerById(@PathVariable Long id){
        return customerService.findById(id)
                .map(this::resource)
                .map(this::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping(value = "/name/{name}")
    public ResponseEntity<Resources<Resource<Customer>>> getCustomersByName(@PathVariable("name") String name){

        Resources<Resource<Customer>> resources = new Resources<>(
                customerService.findAllUsersWithName(name)
                        .map(this::resource)
                        .collect(Collectors.toList())
        );
        addFindAllCustomersLink(resources, REL_ALL_CUSTOMERS);
        return ok(resources);
    }

    @PostMapping
    public ResponseEntity<?> addCustomer(@RequestBody Customer customer){
        Customer addedCustomer = customerService.save(customer);
        return ResponseEntity.created(URI.create(
                resource(addedCustomer).getLink(REL_SELF).getHref())).build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCustomerById(@PathVariable Long id){
        Customer customer = customerService.findById(id).orElse(null);
        if(customer == null){
            return new ResponseEntity<>(
                    new ResourceNotFoundException("Customer no : " + id + " not found")
                            .getMessage(), HttpStatus.NOT_FOUND);
        }
        customerService.deleteById(id);
        return new ResponseEntity<Customer>(HttpStatus.NO_CONTENT);
    }

    private Resource<Customer> resource(Customer customer){
        Resource<Customer> customerResource = new Resource<>(customer);
        customerResource
                .add(linkTo(methodOn(CustomerController.class)
                .getCustomerById(customer.getId()))
                .withSelfRel());
        return customerResource;
    }

    private <T> ResponseEntity<T> ok(T body){
        return ResponseEntity.ok().body(body);
    }

    private void addFindAllCustomersLink(Resources<Resource<Customer>> resources, String rel){
        resources
                .add(linkTo(methodOn(CustomerController.class).getAllCustomers())
                .withRel(rel));
    }

    private void addFindCustomerByIdLink(Resources<Resource<Customer>> resources, String rel){
        resources.add(linkTo(methodOn(CustomerController.class)
                .getCustomerById(null)).withRel(rel));
    }

}
