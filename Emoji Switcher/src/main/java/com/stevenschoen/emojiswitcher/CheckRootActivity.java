package com.stevenschoen.emojiswitcher;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class CheckRootActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkroot);

        Button buttonTryAgain = (Button) findViewById(R.id.button_tryagain);
        buttonTryAgain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (EmojiSwitcherUtils.isRootReady()) {
                    Intent switcherIntent = new Intent(CheckRootActivity.this, SwitcherActivity.class);
                    startActivity(switcherIntent);
                } else {
                    Toast.makeText(CheckRootActivity.this, getString(R.string.failed_to_get_root), Toast.LENGTH_LONG).show();
                }
            }
        });
    }
}