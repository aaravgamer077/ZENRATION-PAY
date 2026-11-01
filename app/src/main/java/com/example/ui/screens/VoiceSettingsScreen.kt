package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.components.SimulatedBadgePill
import com.example.ui.theme.ZenBlack
import com.example.ui.theme.ZenCardBorder
import com.example.ui.theme.ZenDeepPurple
import com.example.ui.theme.ZenElectricBlue
import com.example.ui.theme.ZenPurpleGlow
import com.example.ui.theme.ZenRoyalPurple
import com.example.ui.theme.ZenSimAmber
import com.example.ui.theme.ZenSuccessGreen
import com.example.ui.theme.ZenSurfaceDark
import com.example.ui.theme.ZenSurfaceElevated
import com.example.ui.theme.ZenTextMuted
import com.example.ui.theme.ZenTextPrimary
import com.example.ui.theme.ZenTextSecondary
import com.example.voice.TtsEngineStatus
import com.example.voice.VoiceAnnouncementManager
import com.example.voice.VoiceLanguage
import com.example.voice.VoicePreferences
import kotlin.math.roundToInt

@Composable
fun VoiceSettingsScreen(
  voicePreferences: VoicePreferences,
  engineStatus: TtsEngineStatus,
  isSpeaking: Boolean,
  lastSpokenText: String?,
  onToggleEnabled: (Boolean) -> Unit,
  onSelectLanguage: (VoiceLanguage) -> Unit,
  onVolumeChange: (Float) -> Unit,
  onTestVoice: (Double) -> Unit,
  modifier: Modifier = Modifier,
) {
  var customTestAmount by remember { mutableStateOf("150") }

  val infiniteTransition = rememberInfiniteTransition(label = "soundbox_wave")
  val waveScale by
    infiniteTransition.animateFloat(
      initialValue = 0.85f,
      targetValue = 1.2f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(600, easing = FastOutSlowInEasing),
          repeatMode = RepeatMode.Reverse,
        ),
      label = "wave_scale",
    )

  LazyColumn(
    modifier = modifier.fillMaxSize().background(ZenBlack).testTag("voice_settings_screen"),
    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
    verticalArrangement = Arrangement.spacedBy(18.dp),
  ) {
    // 1. Header
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Zen Soundbox Voice",
            style = MaterialTheme.typography.headlineMedium,
            color = ZenTextPrimary,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text = "Dynamic Payment Simulation Voice Alert System",
            style = MaterialTheme.typography.bodySmall,
            color = ZenTextSecondary,
          )
        }
        SimulatedBadgePill(
          text = if (voicePreferences.isEnabled) "VOICE ON" else "MUTED",
          accentColor = if (voicePreferences.isEnabled) ZenElectricBlue else ZenSimAmber,
        )
      }
    }

    // 2. Hero Soundbox Visualizer & Master Enable/Disable Card
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.2.dp, ZenElectricBlue.copy(alpha = 0.5f)),
      ) {
        Box(
          modifier =
            Modifier
              .fillMaxWidth()
              .background(
                brush =
                  Brush.linearGradient(
                    colors =
                      listOf(
                        Color(0xFF1D0D46),
                        Color(0xFF120B2B),
                        Color(0xFF061B2A),
                      )
                  )
              )
              .padding(20.dp)
        ) {
          Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
              ) {
                Box(
                  modifier =
                    Modifier
                      .size(54.dp)
                      .clip(CircleShape)
                      .background(
                        brush =
                          Brush.linearGradient(
                            colors = listOf(ZenDeepPurple, ZenElectricBlue)
                          )
                      ),
                  contentAlignment = Alignment.Center,
                ) {
                  Icon(
                    imageVector =
                      if (isSpeaking) Icons.Default.GraphicEq else Icons.Default.RecordVoiceOver,
                    contentDescription = null,
                    tint = Color.White,
                    modifier =
                      Modifier.size(if (isSpeaking) (26 * waveScale).dp else 26.dp),
                  )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                  Text(
                    text = "Voice Announcement",
                    style = MaterialTheme.typography.titleLarge,
                    color = ZenTextPrimary,
                    fontWeight = FontWeight.Bold,
                  )
                  Text(
                    text =
                      if (voicePreferences.isEnabled) {
                        "Speaks exact amount after every simulation"
                      } else {
                        "Voice announcements are currently muted"
                      },
                    style = MaterialTheme.typography.bodySmall,
                    color = ZenTextSecondary,
                  )
                }
              }

              Switch(
                checked = voicePreferences.isEnabled,
                onCheckedChange = onToggleEnabled,
                modifier = Modifier.testTag("voice_enable_switch"),
                colors =
                  SwitchDefaults.colors(
                    checkedThumbColor = ZenBlack,
                    checkedTrackColor = ZenElectricBlue,
                  ),
              )
            }

            HorizontalDivider(color = ZenCardBorder.copy(alpha = 0.6f))

            // TTS Engine Status Row
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              val (statusText, statusColor) =
                when (engineStatus) {
                  is TtsEngineStatus.Initializing -> "Initializing Android TTS..." to ZenSimAmber
                  is TtsEngineStatus.Ready ->
                    "TTS Engine Ready • ${engineStatus.languageLabel}" to ZenSuccessGreen
                  is TtsEngineStatus.Unavailable -> engineStatus.reason to ZenSimAmber
                }
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Default.CheckCircle,
                  contentDescription = null,
                  tint = statusColor,
                  modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = statusText,
                  style = MaterialTheme.typography.labelSmall,
                  color = statusColor,
                )
              }
            }

            // Live Phrase Preview Box
            val sampleAmountVal = customTestAmount.toDoubleOrNull()?.takeIf { it > 0 } ?: 150.0
            val previewSentence =
              VoiceAnnouncementManager.buildAnnouncementSentence(
                amount = sampleAmountVal,
                language = voicePreferences.language,
              )
            Surface(
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(14.dp),
              color = ZenBlack.copy(alpha = 0.6f),
              border = BorderStroke(1.dp, ZenCardBorder),
            ) {
              Column(modifier = Modifier.padding(12.dp)) {
                Text(
                  text =
                    if (isSpeaking) "SPEAKING NOW:" else "LIVE VOICE OUTPUT FORMAT:",
                  style = MaterialTheme.typography.labelSmall,
                  color = ZenElectricBlue,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = "\"${lastSpokenText ?: previewSentence}\"",
                  style = MaterialTheme.typography.titleSmall,
                  color = ZenTextPrimary,
                  fontWeight = FontWeight.SemiBold,
                  modifier = Modifier.testTag("voice_live_preview_text"),
                )
              }
            }
          }
        }
      }
    }

    // 3. Language Selection Card (Hindi Voice & English Voice)
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = ZenSurfaceDark),
        border = BorderStroke(1.dp, ZenCardBorder),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(18.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Language,
              contentDescription = null,
              tint = ZenElectricBlue,
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = "Select Announcement Voice",
                style = MaterialTheme.typography.titleMedium,
                color = ZenTextPrimary,
                fontWeight = FontWeight.Bold,
              )
              Text(
                text = "Saved automatically to your device preferences",
                style = MaterialTheme.typography.bodySmall,
                color = ZenTextSecondary,
              )
            }
          }

          VoiceLanguage.entries.forEach { lang ->
            val isSelected = voicePreferences.language == lang
            Surface(
              modifier =
                Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(16.dp))
                  .clickable { onSelectLanguage(lang) }
                  .testTag("voice_language_${lang.code}"),
              shape = RoundedCornerShape(16.dp),
              color =
                if (isSelected) ZenRoyalPurple.copy(alpha = 0.22f) else ZenSurfaceElevated,
              border =
                BorderStroke(
                  width = if (isSelected) 1.5.dp else 1.dp,
                  color = if (isSelected) ZenElectricBlue else ZenCardBorder,
                ),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                RadioButton(
                  selected = isSelected,
                  onClick = { onSelectLanguage(lang) },
                  colors =
                    RadioButtonDefaults.colors(
                      selectedColor = ZenElectricBlue,
                      unselectedColor = ZenTextMuted,
                    ),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                  ) {
                    Text(
                      text = lang.displayName,
                      style = MaterialTheme.typography.titleSmall,
                      color = ZenTextPrimary,
                      fontWeight = FontWeight.Bold,
                    )
                    SimulatedBadgePill(
                      text = lang.nativeBadge,
                      accentColor = if (isSelected) ZenElectricBlue else ZenPurpleGlow,
                    )
                  }
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(
                    text = lang.samplePreview,
                    style = MaterialTheme.typography.bodySmall,
                    color = ZenTextSecondary,
                  )
                }
              }
            }
          }
        }
      }
    }

    // 4. Volume Control Card
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = ZenSurfaceDark),
        border = BorderStroke(1.dp, ZenCardBorder),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(18.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          val volumePercent = (voicePreferences.volume * 100).roundToInt()
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector =
                  if (voicePreferences.volume <= 0.01f) {
                    Icons.AutoMirrored.Filled.VolumeOff
                  } else {
                    Icons.AutoMirrored.Filled.VolumeUp
                  },
                contentDescription = null,
                tint = ZenElectricBlue,
              )
              Spacer(modifier = Modifier.width(10.dp))
              Text(
                text = "Announcement Volume",
                style = MaterialTheme.typography.titleMedium,
                color = ZenTextPrimary,
                fontWeight = FontWeight.Bold,
              )
            }

            Surface(
              shape = RoundedCornerShape(10.dp),
              color = ZenElectricBlue.copy(alpha = 0.16f),
              border = BorderStroke(1.dp, ZenElectricBlue.copy(alpha = 0.4f)),
            ) {
              Text(
                text = "$volumePercent%",
                style = MaterialTheme.typography.labelLarge,
                color = ZenElectricBlue,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
              )
            }
          }

          Slider(
            value = voicePreferences.volume,
            onValueChange = onVolumeChange,
            valueRange = 0f..1f,
            modifier = Modifier.fillMaxWidth().testTag("voice_volume_slider"),
            colors =
              SliderDefaults.colors(
                thumbColor = ZenElectricBlue,
                activeTrackColor = ZenElectricBlue,
                inactiveTrackColor = ZenCardBorder,
              ),
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            listOf(0.25f to "25%", 0.5f to "50%", 0.75f to "75%", 1.0f to "100% Max").forEach {
              (valFloat, label) ->
              Surface(
                modifier =
                  Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onVolumeChange(valFloat) },
                shape = RoundedCornerShape(10.dp),
                color = ZenSurfaceElevated,
                border = BorderStroke(1.dp, ZenCardBorder),
              ) {
                Text(
                  text = label,
                  style = MaterialTheme.typography.labelSmall,
                  color = ZenTextSecondary,
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
              }
            }
          }
        }
      }
    }

    // 5. Dynamic Test Voice Studio Card (₹25, ₹150, ₹750, ₹2,000 + Custom Amount)
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = ZenSurfaceDark),
        border = BorderStroke(1.dp, ZenCardBorder),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(18.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          Text(
            text = "Test Dynamic Voice Announcement",
            style = MaterialTheme.typography.titleMedium,
            color = ZenTextPrimary,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text =
              "Tap any sample amount below or enter your own amount to hear the exact dynamic voice announcement:",
            style = MaterialTheme.typography.bodySmall,
            color = ZenTextSecondary,
          )

          // Quick Example Chips matching specification: ₹25, ₹150, ₹750, ₹2,000
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            listOf(
              25.0 to "₹25",
              150.0 to "₹150",
              750.0 to "₹750",
              2000.0 to "₹2,000",
            ).forEach { (amt, label) ->
              Surface(
                modifier =
                  Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                      customTestAmount = amt.toInt().toString()
                      onTestVoice(amt)
                    }
                    .testTag("test_voice_chip_${amt.toInt()}"),
                shape = RoundedCornerShape(12.dp),
                color = ZenSurfaceElevated,
                border = BorderStroke(1.dp, ZenElectricBlue.copy(alpha = 0.45f)),
              ) {
                Column(
                  modifier = Modifier.padding(vertical = 10.dp),
                  horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                  Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = ZenElectricBlue,
                    fontWeight = FontWeight.Bold,
                  )
                  Text(
                    text = "Speak",
                    style = MaterialTheme.typography.labelSmall,
                    color = ZenPurpleGlow,
                  )
                }
              }
            }
          }

          OutlinedTextField(
            value = customTestAmount,
            onValueChange = { input ->
              if (input.all { it.isDigit() || it == '.' }) {
                customTestAmount = input
              }
            },
            label = { Text("Custom Test Amount (₹)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth().testTag("test_voice_custom_amount_input"),
            shape = RoundedCornerShape(14.dp),
            colors =
              OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ZenElectricBlue,
                unfocusedBorderColor = ZenCardBorder,
                focusedTextColor = ZenTextPrimary,
                unfocusedTextColor = ZenTextPrimary,
              ),
          )

          Button(
            onClick = {
              val amt = customTestAmount.toDoubleOrNull()?.takeIf { it > 0 } ?: 150.0
              onTestVoice(amt)
            },
            modifier =
              Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("test_voice_button"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            contentPadding = PaddingValues(),
          ) {
            Box(
              modifier =
                Modifier
                  .fillMaxSize()
                  .background(
                    brush =
                      Brush.horizontalGradient(
                        colors = listOf(ZenDeepPurple, ZenRoyalPurple, ZenElectricBlue)
                      ),
                    shape = RoundedCornerShape(16.dp),
                  ),
              contentAlignment = Alignment.Center,
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
              ) {
                Icon(
                  imageVector = Icons.Default.PlayArrow,
                  contentDescription = null,
                  tint = ZenTextPrimary,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "TEST VOICE ANNOUNCEMENT",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                  color = ZenTextPrimary,
                )
              }
            }
          }
        }
      }
    }

    // 6. Safety & Compliance Notice
    item {
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = ZenDeepPurple.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, ZenRoyalPurple.copy(alpha = 0.4f)),
      ) {
        Row(
          modifier = Modifier.padding(14.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
            imageVector = Icons.Default.Shield,
            contentDescription = null,
            tint = ZenElectricBlue,
            modifier = Modifier.size(20.dp),
          )
          Spacer(modifier = Modifier.width(10.dp))
          Text(
            text =
              "All voice announcements explicitly state \"payment simulation complete\" so they can never be mistaken for a real payment confirmation.",
            style = MaterialTheme.typography.bodySmall,
            color = ZenTextSecondary,
          )
        }
      }
    }
  }
}
