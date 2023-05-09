package com.example.dialectica.ui.home

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.dialectica.database.room.AppRoomDatabase
import com.example.dialectica.database.room.AppRoomRepository
import com.example.dialectica.models.DialectQuestion
import com.example.dialectica.models.DialectTheme
import com.example.dialectica.models.Themes
import com.example.dialectica.utils.REPOSITORY
import com.example.dialectica.utils.TAG
import com.example.dialectica.utils.TYPE_ROOM
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


class HomeViewModel(application: Application): AndroidViewModel(application) {

    private val context = application

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    init {
        _uiState.update {
            it.copy(
                themeList = Themes().themeList,
                allQuestions = Themes().questionList
            )
        }
    }

    fun initDatabase(type: String, onSuccess: () -> Unit) {
        when(type) {
            TYPE_ROOM -> {
                val dao = AppRoomDatabase.getInstance(context).getAppRoomDao()
                REPOSITORY = AppRoomRepository(dao)
                // callback about completed
                onSuccess()
            }
        }
    }

    fun onClickTheme(theme: DialectTheme) {
        Log.d(this.TAG, "onClickTheme")
        val themes = _uiState.value.themeList.map {
            it.isChosen = it.id == theme.id
            it
        }
        val questions = mutableListOf<DialectQuestion>()
        Themes().questionList.forEach {
            if (it.idTheme == theme.id) questions.add(it)
        }
        val newQuestion = questions.random()

        _uiState.update {
            it.copy(
                themeList = themes,
                currentQuestionList = questions,
                currentQuestion = newQuestion,
                isFavourite = checkFavourite(newQuestion)
            )
        }
    }

    fun onClickNext() {
        Log.d(TAG, "onClickNext")
        var nextQuestion = _uiState.value.currentQuestion
        while (nextQuestion == _uiState.value.currentQuestion) {
            nextQuestion = _uiState.value.currentQuestionList.random()
        }

        _uiState.update {
            it.copy(
                currentQuestion = nextQuestion,
                isFavourite = checkFavourite(nextQuestion)
            )
        }
    }

    fun onClickRandom(): DialectQuestion? {
        var randomQuestion = _uiState.value.currentRandomQuestion
        while (randomQuestion == _uiState.value.currentRandomQuestion || randomQuestion == _uiState.value.currentQuestion) {
            randomQuestion = _uiState.value.allQuestions.random()
        }
        _uiState.update {
            it.copy(
                currentRandomQuestion = randomQuestion
            )
        }
        return randomQuestion
    }

    fun addToFavourite(question: DialectQuestion?, onSuccess: () -> Unit) {
        Log.d(TAG, "addToFavourite")
        if (checkFavourite(question)) return

        _uiState.update { it.copy(isFavourite = true) }

        viewModelScope.launch(Dispatchers.Main) {
            REPOSITORY.insertFavourite(question)
            getFavQuestions()
            onSuccess()
        }
    }

    private fun checkFavourite(question: DialectQuestion?): Boolean {
        Log.d(TAG, "checkFavourite: $question")
        var isFavourite = false
        _uiState.value.favouriteList.forEach {
            if (it.text == question?.text) isFavourite = true
        }
        return isFavourite
    }

    fun getFavQuestions() {
        Log.d(TAG, "getFavQuestions")
        viewModelScope.launch(Dispatchers.Main) {
            _uiState.update {
                it.copy(
                    favouriteList = REPOSITORY.getFavouriteList()
                )
            }
        }
    }

    fun addToPersonal(currentQuestion: DialectQuestion?, function: () -> Unit) {

    }
}

data class HomeUiState(
    val themeList: List<DialectTheme> = emptyList(),
    val allQuestions: List<DialectQuestion> = emptyList(),
    val currentQuestionList: List<DialectQuestion> = emptyList(),
    val favouriteList: List<DialectQuestion> = emptyList(),
    val currentQuestion: DialectQuestion? = null,
    val currentRandomQuestion: DialectQuestion? = null,
    val isFavourite: Boolean = false
)
