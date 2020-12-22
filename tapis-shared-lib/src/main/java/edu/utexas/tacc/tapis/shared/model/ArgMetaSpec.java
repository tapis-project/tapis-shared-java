package edu.utexas.tacc.tapis.shared.model;

import java.util.List;

public class ArgMetaSpec 
{
    private String               name;
    private String               description;
    private Boolean              required;
    private List<KeyValueString> kv;
    
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public Boolean getRequired() {
        return required;
    }
    public void setRequired(Boolean required) {
        this.required = required;
    }
    public List<KeyValueString> getKv() {
        return kv;
    }
    public void setKv(List<KeyValueString> kv) {
        this.kv = kv;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
}
