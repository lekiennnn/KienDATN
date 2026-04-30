package com.example.nam.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.nam.R
import com.example.nam.data.User
import com.example.nam.data.FriendshipStatus
import com.example.nam.ui.theme.LocalCustomColors

@Composable
fun ProfileHeader(
    user: User,
    postCount: String = "0",
    isLoading: Boolean = false,
    isOwnProfile: Boolean = true,
    onImageClick: () -> Unit = {},
    friendshipStatus: FriendshipStatus? = null,
    isFriendRequestInProgress: Boolean = false,
    onAddFriendClick: () -> Unit = {},
    onRemoveFriendClick: () -> Unit = {},
    onCancelRequestClick: () -> Unit = {},
    isRequestSender: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // User avatar
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                .clickable { onImageClick() }
        ) {
            if (user.photoUrl != null) {
                AsyncImage(
                    model = user.photoUrl,
                    contentDescription = "Profile picture",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Show first letter of name as avatar
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(70.dp)
                )
            }

            // Show loading indicator when updating the image
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(40.dp),
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // User display name
        Text(
            text = user.displayName,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = LocalCustomColors.current.textPrimary
        )

        // User email
        Text(
            text = user.email,
            fontSize = 16.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Bio
        if (!user.bio.isNullOrEmpty()) {
            Text(
                text = user.bio,
                fontSize = 16.sp,
                modifier = Modifier.padding(vertical = 8.dp),
                color = LocalCustomColors.current.textPrimary
            )
        }

        // Show friend request button when viewing someone else's profile
        if (!isOwnProfile) {
            when (friendshipStatus) {
                FriendshipStatus.ACCEPTED -> {
                    OutlinedButton(
                        onClick = onRemoveFriendClick,
                        enabled = !isFriendRequestInProgress,
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        if (isFriendRequestInProgress) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(stringResource(R.string.remove_friend))
                    }
                }

                FriendshipStatus.PENDING -> {
                    OutlinedButton(
                        onClick = {},
                        enabled = false,
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        Text(stringResource(R.string.request_pending))
                    }

                    OutlinedButton(
                        onClick = onCancelRequestClick,
                        enabled = !isFriendRequestInProgress,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        if (isFriendRequestInProgress) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(stringResource(R.string.cancel_request))
                    }
                }

                FriendshipStatus.DECLINED -> {
                    Button(
                        onClick = onAddFriendClick,
                        enabled = !isFriendRequestInProgress,
                        modifier = Modifier.padding(vertical = 8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        if (isFriendRequestInProgress) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(stringResource(R.string.add_friend))
                    }
                }

                else -> {
                    Button(
                        onClick = onAddFriendClick,
                        enabled = !isFriendRequestInProgress,
                        modifier = Modifier.padding(vertical = 8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        if (isFriendRequestInProgress) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(stringResource(R.string.add_friend))
                    }
                }
            }
        }

        // Stats row (could be expanded later)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            StatItem(
                value = postCount,
                label = stringResource(R.string.posts),
                modifier = Modifier.weight(1f)
            )
            // Display friend count from the user object
            StatItem(
                value = user.friendCount.toString(),
                label = stringResource(R.string.friends),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun StatItem(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Text(
            text = value,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = LocalCustomColors.current.textPrimary
        )

        Text(
            text = label,
            color = Color.Gray,
            fontSize = 14.sp
        )
    }
}