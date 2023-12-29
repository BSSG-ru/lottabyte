package ru.bssg.lottabyte.coreapi.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.bssg.lottabyte.core.model.backupRun.BackupRun;
import ru.bssg.lottabyte.core.model.backupRun.UpdatableBackupRunEntity;
import ru.bssg.lottabyte.coreapi.repository.BackupRunRepository;

@Service
@Slf4j
@RequiredArgsConstructor
public class BackupRunService {
    private final BackupRunRepository backupRunRepository;

    public BackupRun getBackupRunById(String backupRunId) {
        return backupRunRepository.getById(backupRunId, null);
    }

    public BackupRun updateBackupRunById(String backupRunId, UpdatableBackupRunEntity updatableBackupRunEntity) {
        backupRunRepository.updateBackupRunById(backupRunId, updatableBackupRunEntity);
        return getBackupRunById(backupRunId);
    }

    public BackupRun createBackupRun(UpdatableBackupRunEntity updatableBackupRunEntity) {
        String backupRunId = backupRunRepository.createBackupRun(updatableBackupRunEntity);
        return getBackupRunById(backupRunId);
    }
}
