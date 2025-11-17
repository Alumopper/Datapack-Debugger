package net.gunivers.sniffer.util

import com.mojang.brigadier.StringReader
import com.mojang.brigadier.exceptions.CommandSyntaxException

object StringHelper {
    class StringReaderTester(val reader: StringReader, val cursor: Int = reader.cursor) {

        private var result = true

        fun expect(char: Char): StringReaderTester {
            if(!result) return this
            return if(reader.canRead() && reader.peek() == char){
                reader.cursor++
                this
            }else{
                this.result = false
                this
            }
        }

        fun expect(str: String): StringReaderTester {
            if(!result) return this
            return if(reader.canRead(str.length) && reader.string.substring(reader.cursor, reader.cursor + str.length) == str){
                reader.cursor += str.length
                this
            }else{
                this.result = false
                this
            }
        }

        fun skipWhitespace(): StringReaderTester {
            if(!result) return this
            reader.skipWhitespace()
            return this
        }

        fun result(): Boolean{
            reader.cursor = cursor
            return result
        }
    }

    fun StringReader.backUtil(stop: Char){
        while(this.canRead() && this.peek() != stop){
            this.cursor--
        }
    }

    fun StringReader.readWord(): String{
        val start = this.cursor
        while(this.canRead() && !this.peek().isWhitespace()){
            this.cursor++
        }
        return this.string.substring(start, this.cursor)
    }

    fun StringReader.readUntil(stop: Char): String {
        val start = this.cursor
        while(this.canRead() && this.peek() != stop){
            this.cursor++
        }
        return this.string.substring(start, this.cursor)
    }

    fun StringReader.test(): StringReaderTester = StringReaderTester(this)

    fun StringReader.test(expected: (StringReader) -> Boolean): Boolean {
        val curr = this.cursor
        val qwq = expected(this)
        this.cursor = curr
        return qwq
    }

    fun StringReader.test(expected: Char): Boolean{
        return this.canRead() && this.peek() == expected
    }

    fun StringReader.test(expected: String): Boolean{
        return this.canRead(expected.length) && this.string.substring(this.cursor, this.cursor + expected.length) == expected
    }

    fun StringReader.expect(expected: String){
        val qwq = this.string.substring(this.cursor, this.cursor + expected.length) != expected
        if(qwq){
            throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.readerExpectedSymbol()
                .createWithContext(this, expected)
        }
        this.cursor += expected.length
    }
}