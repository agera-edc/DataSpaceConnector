package org.eclipse.dataspaceconnector.extensions.api;

import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.DataSource;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.DataSourceFactory;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataFlowRequest;
import org.jetbrains.annotations.NotNull;

import java.io.File;

class FileTransferDataSourceFactory implements DataSourceFactory {
    @Override
    public boolean canHandle(DataFlowRequest dataRequest) {
        return "file".equalsIgnoreCase(dataRequest.getSourceDataAddress().getType());
    }

    @Override
    public @NotNull Result<Boolean> validate(DataFlowRequest request) {
        var source = getFile(request);
        if (!source.exists()) {
            return Result.failure("Source file " + source.getName() + " does not exist!");
        }

        return Result.success(true);
    }

    @Override
    public DataSource createSource(DataFlowRequest request) {
        var source = getFile(request);
        return new FileTransferDataSource(source);
    }

    @NotNull
    private File getFile(DataFlowRequest request) {
        var dataAddress = request.getSourceDataAddress();
        // verify source path
        String sourceFileName = dataAddress.getProperty("filename");
        String path = dataAddress.getProperty("path");
        // Make CodeQL happy
        if (path.contains("..")) {
            throw new EdcException("Unsafe path");
        }
        return new File(path, sourceFileName);
    }
}
