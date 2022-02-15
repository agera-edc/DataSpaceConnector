package org.eclipse.dataspaceconnector.dataplane.azurestorage.adapter;

import com.azure.storage.blob.specialized.BlockBlobClient;

import java.io.InputStream;
import java.io.OutputStream;

public class DefaultBlobAdapter implements BlobAdapter {
    private final BlockBlobClient client;

    public DefaultBlobAdapter(BlockBlobClient client) {
        this.client = client;
    }

    @Override
    public OutputStream getOutputStream() {
        return client.getBlobOutputStream(/* overwrite = */ true);
    }

    @Override
    public InputStream openInputStream() {
        return client.openInputStream();
    }

    @Override
    public String getBlobName() {
        return client.getBlobName();
    }

    @Override
    public long getBlobSize() {
        return client.getProperties().getBlobSize();
    }
}
