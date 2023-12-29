package ru.bssg.lottabyte.core.model.backupRun;

import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.Metadata;
import ru.bssg.lottabyte.core.model.ModeledObject;

public class BackupRun extends ModeledObject<BackupRunEntity> {

    public BackupRun() throws LottabyteException {
    }

    public BackupRun(BackupRunEntity entity) throws LottabyteException {
        super(entity);
    }

    public BackupRun(BackupRunEntity entity, Metadata md) throws LottabyteException {
        super(entity, md, ArtifactType.backup_run);
    }

}
