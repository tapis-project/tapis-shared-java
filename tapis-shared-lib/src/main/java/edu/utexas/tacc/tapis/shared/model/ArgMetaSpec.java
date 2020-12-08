package edu.utexas.tacc.tapis.shared.model;

import java.util.List;

public class ArgMetaSpec 
{
    private String               name;
    private boolean              required;
    private List<KeyValueString> kv;
    
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public boolean isRequired() {
        return required;
    }
    public void setRequired(boolean required) {
        this.required = required;
    }
    public List<KeyValueString> getKv() {
        return kv;
    }
    public void setKv(List<KeyValueString> kv) {
        this.kv = kv;
    }
}
