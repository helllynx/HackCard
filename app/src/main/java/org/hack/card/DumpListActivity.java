package org.hack.card;


import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FilenameFilter;

public class DumpListActivity extends AppCompatActivity {

//    protected Toolbar toolbar;
    protected ListView dumpListView;
    protected ArrayAdapter<DumpListAdapter.DumpListFilename> dumpListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dump_list);

        File dumpsDir = getApplicationContext().getExternalFilesDir(null);
        String[] filenames = dumpsDir.list((dir, filename) -> filename.matches(Dump.FILENAME_REGEXP));

//        // setup toolbar
//        toolbar = (Toolbar)findViewById(R.id.toolbar);
//        if (toolbar != null) {
//            toolbar.setTitle(R.string.dumplist_title);
//            toolbar.setSubtitle(R.string.dumplist_subtitle);
//            setSupportActionBar(toolbar);
//            if (getSupportActionBar() != null) {
//                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
//            }
//        }

        // setup list
        dumpListView = (ListView) findViewById(R.id.dumpListView);
        dumpListAdapter = new DumpListAdapter(getApplicationContext(), filenames);

        dumpListView.setAdapter(dumpListAdapter);
        dumpListView.setClickable(true);
        dumpListView.setOnItemClickListener((parent, view, position, id) -> {
            DumpListAdapter.DumpListFilename filename = (DumpListAdapter.DumpListFilename) dumpListView.getItemAtPosition(position);
            String selectedFilename = filename.getFilename();
            Intent intent = new Intent(MainActivity.INTENT_READ_DUMP);
            intent.putExtra("filename", selectedFilename);
            setResult(RESULT_OK, intent);
            finish();
        });
    }
}
