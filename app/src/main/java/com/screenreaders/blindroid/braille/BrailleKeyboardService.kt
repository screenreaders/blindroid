package com.screenreaders.blindroid.braille

import android.inputmethodservice.InputMethodService
import android.view.View
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.ToggleButton
import com.screenreaders.blindroid.R

class BrailleKeyboardService : InputMethodService() {
    private lateinit var statusText: TextView
    private lateinit var dotButtons: List<ToggleButton>
    private lateinit var clearButton: Button
    private lateinit var writeButton: Button
    private lateinit var spaceButton: Button
    private lateinit var backspaceButton: Button
    private lateinit var enterButton: Button
    private lateinit var numberButton: Button
    private lateinit var capsButton: Button
    private lateinit var polishRow: LinearLayout
    private lateinit var polishScroll: HorizontalScrollView

    private var numberMode = false
    private var capsMode = false

    override fun onCreateInputView(): View {
        val view = layoutInflater.inflate(R.layout.keyboard_braille, null)
        statusText = view.findViewById(R.id.brailleStatus)
        dotButtons = listOf(
            view.findViewById(R.id.brailleDot1),
            view.findViewById(R.id.brailleDot2),
            view.findViewById(R.id.brailleDot3),
            view.findViewById(R.id.brailleDot4),
            view.findViewById(R.id.brailleDot5),
            view.findViewById(R.id.brailleDot6)
        )
        clearButton = view.findViewById(R.id.brailleClear)
        writeButton = view.findViewById(R.id.brailleWrite)
        spaceButton = view.findViewById(R.id.brailleSpace)
        backspaceButton = view.findViewById(R.id.brailleBackspace)
        enterButton = view.findViewById(R.id.brailleEnter)
        numberButton = view.findViewById(R.id.brailleNumber)
        capsButton = view.findViewById(R.id.brailleCaps)
        polishRow = view.findViewById(R.id.braillePolishRow)
        polishScroll = view.findViewById(R.id.braillePolishScroll)

        clearButton.setOnClickListener { clearDots() }
        writeButton.setOnClickListener { commitCurrent() }
        spaceButton.setOnClickListener { commitText(" ") }
        backspaceButton.setOnClickListener { currentInputConnection.deleteSurroundingText(1, 0) }
        enterButton.setOnClickListener { currentInputConnection.commitText("\n", 1) }
        numberButton.setOnClickListener {
            numberMode = !numberMode
            if (numberMode) capsMode = false
            updateStatus()
        }
        capsButton.setOnClickListener {
            capsMode = !capsMode
            if (capsMode) numberMode = false
            updateStatus()
        }
        bindPolishButtons()
        updateStatus()
        return view
    }

    private fun bindPolishButtons() {
        val letters = listOf("ą", "ć", "ę", "ł", "ń", "ó", "ś", "ź", "ż")
        polishRow.removeAllViews()
        letters.forEach { letter ->
            val button = Button(this).apply {
                text = letter
                setOnClickListener { commitText(letter) }
            }
            polishRow.addView(button)
        }
    }

    private fun updateStatus() {
        statusText.text = when {
            numberMode -> getString(R.string.braille_status_numbers)
            capsMode -> getString(R.string.braille_status_caps)
            else -> getString(R.string.braille_status_letters)
        }
        numberButton.isSelected = numberMode
        capsButton.isSelected = capsMode
        polishScroll.visibility = View.VISIBLE
    }

    private fun commitCurrent() {
        val pattern = dotPattern()
        val char = if (numberMode) {
            numberMap[pattern]
        } else {
            letterMap[pattern]
        }
        if (char != null) {
            val out = if (capsMode) char.uppercase() else char
            commitText(out)
        }
    }

    private fun commitText(text: String) {
        currentInputConnection.commitText(text, 1)
        clearDots()
    }

    private fun clearDots() {
        dotButtons.forEach { it.isChecked = false }
    }

    private fun dotPattern(): Int {
        var pattern = 0
        dotButtons.forEachIndexed { index, button ->
            if (button.isChecked) {
                pattern = pattern or (1 shl index)
            }
        }
        return pattern
    }

    private val letterMap = mapOf(
        0b000001 to "a",
        0b000011 to "b",
        0b001001 to "c",
        0b011001 to "d",
        0b010001 to "e",
        0b001011 to "f",
        0b011011 to "g",
        0b010011 to "h",
        0b001010 to "i",
        0b011010 to "j",
        0b000101 to "k",
        0b000111 to "l",
        0b001101 to "m",
        0b011101 to "n",
        0b010101 to "o",
        0b001111 to "p",
        0b011111 to "q",
        0b010111 to "r",
        0b001110 to "s",
        0b011110 to "t",
        0b000101 or 0b100000 to "u",
        0b000111 or 0b100000 to "v",
        0b011010 or 0b100000 to "w",
        0b001101 or 0b100000 to "x",
        0b011101 or 0b100000 to "y",
        0b010101 or 0b100000 to "z",
        0b000010 to ",",
        0b000110 to ";",
        0b001010 or 0b000100 to "?",
        0b001010 or 0b000110 to "!",
        0b001010 or 0b000010 to ".",
        0b000100 to "'"
    )

    private val numberMap = mapOf(
        0b000001 to "1",
        0b000011 to "2",
        0b001001 to "3",
        0b011001 to "4",
        0b010001 to "5",
        0b001011 to "6",
        0b011011 to "7",
        0b010011 to "8",
        0b001010 to "9",
        0b011010 to "0"
    )
}
