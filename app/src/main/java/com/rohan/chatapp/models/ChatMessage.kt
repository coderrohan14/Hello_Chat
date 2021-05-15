package com.rohan.chatapp.models

import android.os.Build
import androidx.annotation.RequiresApi
import java.time.LocalDateTime
data class ChatMessage constructor(val date:String="",val time:String="",
                                                                       val toId:String="",val id:String="",val text:String="", val fromId:String="" )