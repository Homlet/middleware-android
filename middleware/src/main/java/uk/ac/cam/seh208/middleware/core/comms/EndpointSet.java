package uk.ac.cam.seh208.middleware.core.comms;

import android.support.annotation.NonNull;
import android.util.ArrayMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import uk.ac.cam.seh208.middleware.common.Polarity;


// TODO: implement Google Room-based persistence of endpoints.
/**
 * Set for efficient storage and retrieval of active endpoint objects
 * in the middleware instance. Also handles persistence of endpoints
 * using the Google Room persistence library.
 */
public class EndpointSet implements Set<Endpoint> {

    private ArrayMap<String, Endpoint> endpointsByName;
    private ArrayMap<Polarity, ArrayList<Endpoint>> endpointsByPolarity;
    private Set<String> names;


    public EndpointSet() {
        endpointsByName = new ArrayMap<>();
        endpointsByPolarity = new ArrayMap<>();
        for (Polarity polarity : Polarity.values()) {
            endpointsByPolarity.put(polarity, new ArrayList<>());
        }
        names = endpointsByName.keySet();
    }

    /**
     * Return the endpoint object having a particular unique name as
     * stored in the endpoint set.
     *
     * @param name Unique string name of the endpoint.
     *
     * @return an endpoint object.
     */
    public Endpoint getEndpointByName(String name) {
        return endpointsByName.get(name);
    }

    /**
     * Return an unmodifiable view of a list of all endpoints having a
     * particular polarity. Note that whilst directly unmodifiable, this
     * list may change as the endpoint set is interacted with. Therefore,
     * to iterate over the list the endpoint set must be synchronised.
     *
     * @param polarity Endpoint polarity to filter by.
     *
     * @return an unmodifiable list of endpoint objects.
     */
    public List<Endpoint> getEndpointsByPolarity(Polarity polarity) {
        return Collections.unmodifiableList(endpointsByPolarity.get(polarity));
    }

    @Override
    public int size() {
        return names.size();
    }

    @Override
    public boolean isEmpty() {
        return names.isEmpty();
    }

    @Override
    public synchronized boolean contains(Object obj) {
        if (obj == null || !(obj instanceof Endpoint)) {
            // If the passed object is null, or is not an endpoint then
            // it cannot reside in the set.
            return false;
        }

        Endpoint endpoint = (Endpoint) obj;

        if (!names.contains(endpoint.getName())) {
            // If the name of the endpoint is not in the key set, then
            // the endpoint cannot be in the set.
            return false;
        }

        // Return whether the endpoint by the same name in the set is
        // actually equal to the passed endpoint (otherwise a name has
        // illegally been used twice).
        return endpointsByName.get(endpoint.getName()).equals(endpoint);
    }

    @NonNull
    @Override
    public Iterator<Endpoint> iterator() {
        return endpointsByName.values().iterator();
    }

    @NonNull
    @Override
    public Object[] toArray() {
        return endpointsByName.values().toArray();
    }

    @NonNull
    @Override
    public <T> T[] toArray(@NonNull T[] arr) {
        //noinspection SuspiciousToArrayCall
        return endpointsByName.values().toArray(arr);
    }

    @Override
    public synchronized boolean add(Endpoint endpoint) {
        if (endpoint == null || contains(endpoint)) {
            // If the endpoint is null, or an endpoint with that name is
            // already contained in the set, we cannot add it.
            return false;
        }

        endpointsByName.put(endpoint.getName(), endpoint);
        endpointsByPolarity.get(endpoint.getPolarity()).add(endpoint);
        return true;
    }

    @Override
    public synchronized boolean remove(Object obj) {
        if (!contains(obj)) {
            // If the object in not contained in the set, we cannot remove it.
            return false;
        }

        Endpoint endpoint = (Endpoint) obj;
        endpointsByName.remove(endpoint.getName());
        // TODO: make this more efficient.
        endpointsByPolarity.get(endpoint.getPolarity()).remove(endpoint);
        return true;
    }

    @Override
    public synchronized boolean containsAll(@NonNull Collection<?> collection) {
        for (Object obj : collection) {
            if (!contains(obj)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public synchronized boolean addAll(@NonNull Collection<? extends Endpoint> collection) {
        boolean changed = false;
        for (Endpoint endpoint : collection) {
            if (add(endpoint)) {
                changed = true;
            }
        }
        return changed;
    }

    @Override
    public synchronized boolean retainAll(@NonNull Collection<?> collection) {
        boolean changed = false;
        for (Endpoint endpoint : endpointsByName.values()) {
            if (collection.contains(endpoint)) {
                continue;
            }
            remove(endpoint);
            changed = true;
        }
        return changed;
    }

    @Override
    public synchronized boolean removeAll(@NonNull Collection<?> collection) {
        boolean changed = false;
        for (Object obj : collection) {
            if (remove(obj)) {
                changed = true;
            }
        }
        return changed;
    }

    @Override
    public void clear() {
        endpointsByName.clear();
        for (List<Endpoint> list : endpointsByPolarity.values()) {
            list.clear();
        }
    }
}
