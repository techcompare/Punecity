package com.pranav.punecityguide.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pranav.punecityguide.ui.theme.BuzzCard
import com.pranav.punecityguide.ui.theme.BuzzPrimary
import com.pranav.punecityguide.ui.theme.BuzzTextMuted

@Composable
fun PrivacyPolicyScreen(
    onBack: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = BuzzPrimary
                    )
                }
                Text(
                    "Privacy Policy",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black
                )
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = BuzzCard),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(BuzzPrimary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Security,
                                contentDescription = null,
                                tint = BuzzPrimary
                            )
                        }
                        Spacer(modifier = Modifier.size(12.dp))
                        Column {
                            Text(
                                "Your Privacy Matters",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Last updated: April 2026",
                                style = MaterialTheme.typography.labelSmall,
                                color = BuzzTextMuted
                            )
                        }
                    }
                }
            }
        }

        item { PolicySection("1. Information We Collect", """
We collect minimal information to provide you with the best experience:

• **Account Information**: When you create an account, we collect your email address and display name.

• **Usage Data**: We collect anonymous usage statistics to improve the app experience.

• **Community Posts**: Content you voluntarily share in the Community section.

• **Saved Places**: Your saved places are stored locally on your device and synced to your account if logged in.
        """.trimIndent()) }

        item { PolicySection("2. How We Use Your Information", """
Your information is used to:

• Provide and maintain the Pune Buzz service
• Personalize your experience
• Display community posts
• Improve app functionality
• Send important service updates (only if opted in)

We do NOT:
• Sell your personal information
• Share data with third-party advertisers
• Track your precise location without consent
        """.trimIndent()) }

        item { PolicySection("3. Data Storage & Security", """
• Your data is stored securely using industry-standard encryption
• Saved places are stored locally first, then synced to secure cloud servers
• We use Supabase for backend services with enterprise-grade security
• Passwords are hashed and never stored in plain text
        """.trimIndent()) }

        item { PolicySection("4. Your Rights", """
You have the right to:

• **Access**: Request a copy of your data
• **Correction**: Update your account information
• **Deletion**: Delete your account and all associated data
• **Portability**: Export your saved places

To exercise these rights, contact us at workwithme785@gmail.com
        """.trimIndent()) }

        item { PolicySection("5. Third-Party Services", """
We use the following third-party services:

• **Supabase**: For authentication and data storage
• **Google Maps API**: For location and mapping services (when you choose to open maps)

Each service has its own privacy policy that governs data handling.
        """.trimIndent()) }

        item { PolicySection("6. Children's Privacy", """
Pune Buzz is not intended for children under 13 years of age. We do not knowingly collect personal information from children under 13.
        """.trimIndent()) }

        item { PolicySection("7. Changes to This Policy", """
We may update this Privacy Policy from time to time. We will notify you of any changes by posting the new Privacy Policy in the app and updating the "Last updated" date.
        """.trimIndent()) }

        item { PolicySection("8. Contact Us", """
If you have questions about this Privacy Policy, please contact us:

Email: workwithme785@gmail.com
        """.trimIndent()) }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
fun TermsOfServiceScreen(
    onBack: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = BuzzPrimary
                    )
                }
                Text(
                    "Terms of Service",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black
                )
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = BuzzCard),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(BuzzPrimary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Policy,
                                contentDescription = null,
                                tint = BuzzPrimary
                            )
                        }
                        Spacer(modifier = Modifier.size(12.dp))
                        Column {
                            Text(
                                "Terms of Service",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Effective: April 2026",
                                style = MaterialTheme.typography.labelSmall,
                                color = BuzzTextMuted
                            )
                        }
                    }
                }
            }
        }

        item { PolicySection("1. Acceptance of Terms", """
By downloading, installing, or using Pune Buzz, you agree to be bound by these Terms of Service. If you do not agree to these terms, please do not use the app.
        """.trimIndent()) }

        item { PolicySection("2. Description of Service", """
Pune Buzz is a city guide app for Pune, India that provides:

• Discovery of places, cafes, heritage sites, and hidden gems
• Community features for sharing experiences
• Curated itineraries and plans
• Personal collection of saved places

The app is provided "as is" and we continuously work to improve it.
        """.trimIndent()) }

        item { PolicySection("3. User Accounts", """
• You must provide accurate information when creating an account
• You are responsible for maintaining the security of your account
• You must be at least 13 years old to create an account
• Guest mode is available with limited features
        """.trimIndent()) }

        item { PolicySection("4. Acceptable Use", """
You agree NOT to:

• Post false, misleading, or harmful content
• Harass, abuse, or threaten other users
• Impersonate others or misrepresent your identity
• Use the app for illegal purposes
• Attempt to hack or disrupt the service
• Scrape or collect user data without permission
        """.trimIndent()) }

        item { PolicySection("5. Community Guidelines", """
When posting in the Community section:

• Be respectful and constructive
• Share genuine experiences about Pune
• Do not post spam or promotional content
• Do not share personal information of others
• Report inappropriate content using in-app tools

We reserve the right to remove content that violates these guidelines.
        """.trimIndent()) }

        item { PolicySection("6. Intellectual Property", """
• Pune Buzz and its content are protected by copyright
• User-generated content remains owned by users, but you grant us license to display it
• Do not copy or redistribute app content without permission
        """.trimIndent()) }

        item { PolicySection("7. Disclaimers", """
• Place information may change; verify before visiting
• We do not guarantee accuracy of user reviews
• We are not responsible for experiences at listed places
• The app requires internet connectivity for full functionality
        """.trimIndent()) }

        item { PolicySection("8. Limitation of Liability", """
Pune Buzz is provided for informational purposes. We are not liable for:

• Decisions made based on app content
• Experiences at places listed in the app
• Loss of data due to technical issues
• Temporary service unavailability
        """.trimIndent()) }

        item { PolicySection("9. Termination", """
We may terminate or suspend your account if you violate these terms. You may delete your account at any time through the Profile settings.
        """.trimIndent()) }

        item { PolicySection("10. Changes to Terms", """
We may update these Terms of Service. Continued use of the app after changes constitutes acceptance of the new terms.
        """.trimIndent()) }

        item { PolicySection("11. Contact", """
For questions about these Terms, contact us at:

Email: workwithme785@gmail.com
        """.trimIndent()) }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
private fun PolicySection(title: String, content: String) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BuzzCard),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = BuzzPrimary
            )
            Text(
                content,
                style = MaterialTheme.typography.bodySmall,
                color = BuzzTextMuted,
                lineHeight = 20.sp
            )
        }
    }
}
