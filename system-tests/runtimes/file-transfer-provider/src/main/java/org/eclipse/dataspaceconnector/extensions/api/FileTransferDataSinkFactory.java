package org.eclipse.dataspaceconnector.extensions.api;

import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.DataSink;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.DataSinkFactory;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataFlowRequest;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;

class FileTransferDataSinkFactory implements DataSinkFactory {
    private final Monitor monitor;
    private final ExecutorService executorService;
    private final int partitionSize;

    public FileTransferDataSinkFactory(Monitor monitor, ExecutorService executorService, int partitionSize) {
        this.monitor = monitor;
        this.executorService = executorService;
        this.partitionSize = partitionSize;
    }

    @Override
    public boolean canHandle(DataFlowRequest request) {
        return "file".equalsIgnoreCase(request.getDestinationDataAddress().getType());
    }

    @Override
    public @NotNull Result<Boolean> validate(DataFlowRequest request) {
        return Result.success(true);
    }

    @Override
    public DataSink createSink(DataFlowRequest request) {
        var source = request.getSourceDataAddress();
        var destination = request.getDestinationDataAddress();

        // verify destination path
        var destinationFile = new File(destination.getProperty("path"));

        return FileTransferDataSink.Builder.newInstance()
                .file(destinationFile)
                .requestId(request.getId())
                .partitionSize(partitionSize)
                .executorService(executorService)
                .monitor(monitor)
                .build();
    }
}
