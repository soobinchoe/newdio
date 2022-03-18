package com.traydcorp.newdio.ui.player

interface ActionPlaying {
    fun nextClicked (background : String?) {}
    fun prevClicked (background : String?) {}
    fun playClicked () {}
    fun playPrepared (play : Boolean) {}
    fun stopClicked () {}
}