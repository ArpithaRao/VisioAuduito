package com.visio;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.shaded.fasterxml.jackson.annotation.JsonAnyGetter;
import com.shaded.fasterxml.jackson.annotation.JsonAnySetter;
import com.shaded.fasterxml.jackson.annotation.JsonIgnore;
import com.shaded.fasterxml.jackson.annotation.JsonInclude;
import com.shaded.fasterxml.jackson.annotation.JsonProperty;
import com.shaded.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.visio.Zone;

@JsonInclude(JsonInclude.Include.NON_NULL)

@JsonPropertyOrder({
        "Zones"
})
public class Zones {

    @JsonProperty("Zones")
    private List<Zone> Zones = new ArrayList<Zone>();
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    /**
     *
     * @return
     * The Zones
     */
    @JsonProperty("Zones")
    public List<Zone> getZones() {
        return Zones;
    }

    /**
     *
     * @param Zones
     * The Zones
     */
    @JsonProperty("Zones")
    public void setZones(List<Zone> Zones) {
        this.Zones = Zones;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

}