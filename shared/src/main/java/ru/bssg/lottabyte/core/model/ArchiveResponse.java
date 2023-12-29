package ru.bssg.lottabyte.core.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ArchiveResponse {

    private List<String> archivedGuids = new ArrayList();
    private List<String> updatedGuids = new ArrayList();
    private List<String> deletedGuids = new ArrayList();

    public ArchiveResponse() {
    }

    public void addArchivedGuid(String guid) {
        this.archivedGuids.add(guid);
    }

    public void addUpdatedGuid(String guid) {
        this.updatedGuids.add(guid);
    }

    public void addDeletedGuid(String guid) {
        this.deletedGuids.add(guid);
    }

    @Schema(
            name = "archived_guids",
            description = "Guids of the archived objects."
    )
    public List<String> getArchivedGuids() {
        return this.archivedGuids;
    }

    @Schema(
            name = "updated_guids",
            description = "Guids of the updated objects."
    )
    public List<String> getUpdatedGuids() {
        return this.updatedGuids;
    }

    @Schema(
            name = "delete_guids",
            description = "Guids of the deleted objects."
    )
    public List<String> getDeletedGuids() {
        return this.deletedGuids;
    }

}
