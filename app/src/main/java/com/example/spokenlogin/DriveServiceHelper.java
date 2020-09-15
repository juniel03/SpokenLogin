package com.example.spokenlogin;

import android.util.Log;

import androidx.annotation.LongDef;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.api.client.googleapis.batch.BatchRequest;
import com.google.api.client.googleapis.batch.json.JsonBatchCallback;
import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.HttpHeaders;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.Permission;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class DriveServiceHelper {

    private final Executor mExecutor = Executors.newSingleThreadExecutor();
    private Drive mDriveService;
    String fileid = "1jIjVcKHqd-MjR7qWpjyrxZ-hjVahiJOG";

    public DriveServiceHelper(Drive mDriveService){
        this.mDriveService = mDriveService;
    }

    public Task<String> createFilePDF(String filepath){
        return Tasks.call(mExecutor, () ->{

            File fileMetaData = new File();
            fileMetaData.setName("videosample");

            java.io.File file = new java.io.File(filepath);

            FileContent mediaCOntent = new FileContent("application/unknown", file);

            File myfile = null;
            try {
                myfile = mDriveService.files().create(fileMetaData,mediaCOntent).setFields("id, parents").execute();
                System.out.println("File ID: " + myfile.getId());
            }catch (Exception e){
                e.printStackTrace();
            }
            if (myfile == null){
                throw  new IOException(("NUll result when requesting file creation"));
            }
            Log.d("tag", myfile.getId());
            return  myfile.getId();

        });
    }

    public Task<String> pull(){
        return Tasks.call(mExecutor, () ->{
            String file1 = null;
            String pageToken = null;
            do {
                FileList result = null;
                try {
                    result = mDriveService.files().list()
                            .setQ("mimeType != 'application/vnd.google-apps.folder'")
                            .setSpaces("drive")
                            .setFields("nextPageToken, files(id, name)")
                            .setPageToken(pageToken)
                            .execute();
                    Log.d("tag", "try");
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.d("tag", "exception: " + e.toString());
                }
                assert result != null;
                if (result.isEmpty()) {
                    for (File file : result.getFiles()) {
                        System.out.printf("Found file: %s (%s)\n",
                                file.getName(), file.getId());
                    }
                }else {
                    Log.d("tag", "null");
                }
                pageToken = result.getNextPageToken();
            } while (pageToken != null);
            return file1;
        });
    }
    public Task<String> getsharable(){
        return Tasks.call(mExecutor, () ->{
            String file1 = null;
            JsonBatchCallback<Permission> callback = new JsonBatchCallback<Permission>() {
                @Override
                public void onFailure(GoogleJsonError e,
                                      HttpHeaders responseHeaders)
                        throws IOException {
                    // Handle error
                    System.err.println(e.getMessage());
                }

                @Override
                public void onSuccess(Permission permission,
                                      HttpHeaders responseHeaders)
                        throws IOException {
                    System.out.println("Permission ID: " + permission.getId());
                    File mfile = mDriveService.files().get(fileid).setFields("webViewLink").execute();
                    Log.d("tag", "" + mfile.getWebViewLink());

                }
            };
            BatchRequest batch = mDriveService.batch();
            Permission userPermission = new Permission()
                    .setType("anyone")
                    .setRole("writer");
            mDriveService.permissions().create(fileid, userPermission)
                    .queue(batch, callback);
            batch.execute();
            return file1;
        });
    }
}
