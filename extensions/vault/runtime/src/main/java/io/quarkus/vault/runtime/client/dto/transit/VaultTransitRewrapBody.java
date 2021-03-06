package io.quarkus.vault.runtime.client.dto.transit;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.vault.runtime.client.dto.VaultModel;

public class VaultTransitRewrapBody implements VaultModel {

    @JsonProperty("batch_input")
    public List<VaultTransitRewrapBatchInput> batchInput;

}
