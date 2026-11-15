package com.example

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ui.components.HolographicPanel
import com.example.ui.theme.SoloLevelingTheme
import com.example.ui.theme.SystemColors

/**
 * Displays the Health Connect permission rationale when invoked by the system
 * (Settings -> Health Connect -> App permissions -> Rationale / Privacy policy).
 */
class HealthConnectRationaleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        setContent {
            SoloLevelingTheme {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(SystemColors.ObsidianBlack)
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                        .padding(20.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    HolographicPanel(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = "HEALTH CONNECT DATA RATIONALE",
                                style = MaterialTheme.typography.titleLarge,
                                color = SystemColors.ElectricCyan
                            )
                            Text(
                                text = "Solo Leveling — Reality OS requests read-only access to Exercise Sessions, Distance, Heart Rate, and Sleep solely to:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SystemColors.PureIce
                            )
                            Text(
                                text = "• Verify real-world jogging and workout sessions (VERIFIED_HEALTH_CONNECT provenance).\n" +
                                    "• Calculate deterministic STR, VIT, AGI, and DIS attribute progression.\n" +
                                    "• Assess training load and sleep recovery so the System never encourages overtraining.",
                                style = MaterialTheme.typography.bodySmall,
                                color = SystemColors.MetallicSilver
                            )
                            Text(
                                text = "All imported records are stored locally in your device Room database and never fabricated or shared without your explicit sync action.",
                                style = MaterialTheme.typography.bodySmall,
                                color = SystemColors.MutedSilver
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Button(
                                onClick = { finish() },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SystemColors.ElectricCyan,
                                    contentColor = SystemColors.ObsidianBlack
                                )
                            ) {
                                Text("CLOSE", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }
        }
    }
}
