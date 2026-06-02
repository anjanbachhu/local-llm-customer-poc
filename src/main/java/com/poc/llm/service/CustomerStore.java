package com.poc.llm.service;

import com.poc.llm.model.Customer;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Thread-safe in-memory store of the uploaded customer records and the names of
 * the files they came from. For a POC this replaces a database; everything is
 * lost on restart, which is exactly what we want for a stateless demo.
 */
@Component
public class CustomerStore {

    private final List<Customer> customers = Collections.synchronizedList(new ArrayList<>());
    private final Set<String> fileNames = Collections.synchronizedSet(new LinkedHashSet<>());

    /** Add a batch of customers and record the originating file name. */
    public synchronized void addAll(String fileName, List<Customer> incoming) {
        fileNames.add(fileName);
        customers.addAll(incoming);
    }

    /** @return an immutable snapshot of all customers currently held. */
    public List<Customer> getAll() {
        synchronized (customers) {
            return List.copyOf(customers);
        }
    }

    public int count() {
        return customers.size();
    }

    /** @return the distinct, insertion-ordered list of uploaded file names. */
    public List<String> getFileNames() {
        synchronized (fileNames) {
            return List.copyOf(fileNames);
        }
    }

    /** Clear all data (used by the "Reset" action). */
    public synchronized void clear() {
        customers.clear();
        fileNames.clear();
    }
}
