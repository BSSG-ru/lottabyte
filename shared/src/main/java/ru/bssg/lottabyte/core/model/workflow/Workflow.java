package ru.bssg.lottabyte.core.model.workflow;

import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.Metadata;
import ru.bssg.lottabyte.core.model.ModeledObject;

public class Workflow extends ModeledObject<WorkflowEntity> {

    public Workflow () {}

    public Workflow(WorkflowEntity entity) { super(entity); }

    public Workflow(WorkflowEntity entity, Metadata md) { super(entity, md, ArtifactType.workflow); }

}
