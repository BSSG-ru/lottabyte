package ru.bssg.lottabyte.core.util;

import ru.bssg.lottabyte.core.model.ArtifactType;

import java.util.Arrays;

public class WorfklowUtils {

    public ArtifactType[] workflowableArtifactTypes = { ArtifactType.domain };

    public boolean isArtifactTypeWorkflowable(ArtifactType artifactType) {
        return Arrays.asList(workflowableArtifactTypes).stream().anyMatch(x -> x.equals(artifactType));
    }



}
