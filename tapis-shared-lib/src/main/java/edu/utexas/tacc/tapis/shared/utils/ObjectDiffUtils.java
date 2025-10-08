package edu.utexas.tacc.tapis.shared.utils;

import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.Collections;
import java.util.HashMap;
import java.util.stream.Collectors;

/**
 * Utility class to compute differences between two Java objects, lists, or sets.
 * It uses Gson for object serialization and Guava for map difference computation.
 * The differences are categorized into added, removed, and modified fields/elements.
 * 
 * Example usage:
 * ObjectDiff diff = ObjectDiffUtils.computeObjectDiff(oldObject, newObject);
 * ListDiff<String> listDiff = ObjectDiffUtils.computeListDiff(oldList, newList);
 * SetDiff<Integer> setDiff = ObjectDiffUtils.computeSetDiff(oldSet, newSet);
 * 
 * Each diff object can be converted to a JSON string using the toJsonString() method.
 *
 * Sample output for ObjectDiff:
 * {
 *  "addedFields": {"newField": "newValue"},
 *  "removedFields": {"oldField": "oldValue"},
 *  "modifiedFields": {"modifiedField": {"leftValue": "oldValue", "rightValue": "newValue"}}
 * }
 * Sample output for ListDiff:
 * {
 *  "addedElements": {"newElement": 1},
 *  "removedElements": {"oldElement": 1}
 * }
 * Sample output for SetDiff:
 * {
 *  "addedElements": {"newElement"},
 *  "removedElements": {"oldElement"}
 * }
 *
 * @author wei.zhang@tacc.utexas.edu
 */
public class ObjectDiffUtils {

    public static class ListDiff<T> {
        private Map<T, Integer> addedElements;
        private Map<T, Integer> removedElements;

        public Map<T, Integer> getAddedElements() {
            return addedElements;
        }
        public void setAddedElements(Map<T, Integer> addedElements) {
            this.addedElements = addedElements; 
        }

        public Map<T, Integer> getRemovedElements() {
            return removedElements;
        }

        public void setRemovedElements(Map<T, Integer> removedElements) {
            this.removedElements = removedElements;
        }

        public String toJsonString () {
            Gson gson = new Gson();
            return gson.toJson(this);
        }
    }

    public static class SetDiff<T> {
        private Set<T> addedElements;
        private Set<T> removedElements;

        public Set<T> getAddedElements() {
            return addedElements;
        }
        public void setAddedElements(Set<T> addedElements) {
            this.addedElements = addedElements; 
        }

        public Set<T> getRemovedElements() {
            return removedElements;
        }

        public void setRemovedElements(Set<T> removedElements) {
            this.removedElements = removedElements;
        }

        public String toJsonString () {
            Gson gson = new Gson();
            return gson.toJson(this);
        }
    }

    public static class ObjectDiff {
        private Map<String, Object> addedFields;
        private Map<String, Object> removedFields;
        private Map<String, MapDifference.ValueDifference<Object>> modifiedFields;

        public Map<String, Object> getAddedFields() {
            return addedFields;
        }

        public void setAddedFields(Map<String, Object> addedFields) {
            this.addedFields = addedFields;
        }

        public Map<String, Object> getRemovedFields() {
            return removedFields;
        }

        public void setRemovedFields(Map<String, Object> removedFields) {
            this.removedFields = removedFields;
        }

        public Map<String, MapDifference.ValueDifference<Object>> getModifiedFields() {
            return modifiedFields;
        }

        public void setModifiedFields(Map<String, MapDifference.ValueDifference<Object>> modifiedFields) {
            this.modifiedFields = modifiedFields;
        }

        public String toJsonString () {
            Gson gson = new Gson();
            return gson.toJson(this);
        }
    }

    

    public static ObjectDiff computeObjectDiff(Object oldObject, Object newObject) {
        ObjectDiff objDiff = null;
        if (oldObject == null && newObject == null) {
            return null;
        }
        Gson gson = new Gson();
        
        // Use TypeToken to handle generic map types
        Type mapType = new TypeToken<Map<String, Object>>() {}.getType();

        // Convert Java objects to maps
        Map<String, Object> oldMap = oldObject == null ? Collections.emptyMap() : gson.fromJson(gson.toJson(oldObject), mapType);
        Map<String, Object> newMap = newObject == null ? Collections.emptyMap() : gson.fromJson(gson.toJson(newObject), mapType);

        // Find the differences
        MapDifference<String, Object> difference = Maps.difference(oldMap, newMap);
        objDiff = new ObjectDiff();
        objDiff.setAddedFields(difference.entriesOnlyOnRight());
        objDiff.setRemovedFields(difference.entriesOnlyOnLeft());
        objDiff.setModifiedFields(difference.entriesDiffering());
        return objDiff;
    }

    public static <T> SetDiff<T> computeSetDiff(Set<T> oldSet, Set<T> newSet) {
        SetDiff<T> arrayDiff = null; 
        if (oldSet == null && newSet == null) {
            return arrayDiff;
        }
        Set<T> oldSetCopy = (oldSet == null) ? Collections.emptySet() : oldSet;
        Set<T> newSetCopy = (newSet == null) ? Collections.emptySet() : newSet;
        // Find removed elements (in old but not in new)
        Set<T> removed = oldSetCopy.stream()
                .filter(element -> !newSetCopy.contains(element))
                .collect(Collectors.toSet());

        // Find added elements (in new but not in old)
        Set<T> added = newSetCopy.stream()
                .filter(element -> !oldSetCopy.contains(element))
                .collect(Collectors.toSet());

        arrayDiff = new SetDiff<>();
        arrayDiff.setAddedElements(added);
        arrayDiff.setRemovedElements(removed);
        return arrayDiff;
    }

    public static <T> ListDiff<T> computeListDiff(List<T> oldList, List<T> newList) {
        ListDiff<T> listDiff = null;
        if (oldList == null && newList == null) {
            return listDiff;
        }
        // Count frequencies in old and new lists
        Map<T, Integer> oldFreq = new HashMap<>();
        Map<T, Integer> newFreq = new HashMap<>();

        List<T> oldListCopy = (oldList == null) ? Collections.emptyList() : oldList;
        List<T> newListCopy = (newList == null) ? Collections.emptyList() : newList;

        for (T item : oldListCopy) {
            oldFreq.put(item, oldFreq.getOrDefault(item, 0) + 1);
        }

        for (T item : newListCopy) {
            newFreq.put(item, newFreq.getOrDefault(item, 0) + 1);
        }

        // Find added elements (in new but not in old, or more in new than old)
        Map<T, Integer> added = new HashMap<>();
        for (Map.Entry<T, Integer> entry : newFreq.entrySet()) {
            T element = entry.getKey();
            int newCount = entry.getValue();
            int oldCount = oldFreq.getOrDefault(element, 0);
            if (newCount > oldCount) {
                added.put(element, newCount - oldCount);
            }
        }

        // Find removed elements (in old but not in new, or more in old than new)
        Map<T, Integer> removed = new HashMap<>();
        for (Map.Entry<T, Integer> entry : oldFreq.entrySet()) {
            T element = entry.getKey();
            int oldCount = entry.getValue();
            int newCount = newFreq.getOrDefault(element, 0);
            if (oldCount > newCount) {
                removed.put(element, oldCount - newCount);
            }
        }
        listDiff = new ListDiff<>();
        listDiff.setAddedElements(added);
        listDiff.setRemovedElements(removed);
        return listDiff;
    }
}