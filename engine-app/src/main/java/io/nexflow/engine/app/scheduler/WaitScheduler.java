package io.nexflow.engine.app.scheduler;

import io.nexflow.engine.app.runtime.WorkflowRuntimeService;
import io.nexflow.engine.persistence.entity.WaitExecutionEntity;
import io.nexflow.engine.persistence.repository.WaitExecutionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
public class WaitScheduler {

    private final WaitExecutionRepository waitRepo;
    private final WorkflowRuntimeService runtimeService;

    @Scheduled(fixedDelay = 1000)
    public void checkTimeWaits() throws Exception {

        List<WaitExecutionEntity> waits =
                waitRepo.findByStatusAndWaitTypeAndWaitUntilBefore(
                        "WAITING",
                        "TIME",
                        Instant.now()
                );

        for (WaitExecutionEntity wait : waits) {
            wait.setStatus("COMPLETED");
            waitRepo.save(wait);
            runtimeService.resumeWait(wait);
        }
    }
}
