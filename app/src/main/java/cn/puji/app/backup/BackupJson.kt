package cn.puji.app.backup

/** Deterministic UTF-8 JSON: sorted object keys, integer-only numbers, no insignificant space. */
internal object BackupJson {
    fun stringify(value: Any?): String = buildString { write(value) }

    private fun StringBuilder.write(value: Any?) {
        when (value) {
            null -> append("null")
            is String -> {
                append('"')
                value.forEach { c -> when (c) {
                    '"' -> append("\\\"")
                    '\\' -> append("\\\\")
                    '\b' -> append("\\b")
                    '\u000c' -> append("\\f")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> if (c < ' ') append("\\u" + c.code.toString(16).padStart(4, '0')) else append(c)
                } }
                append('"')
            }
            is Long, is Int, is Boolean -> append(value)
            is Map<*, *> -> {
                require(value.keys.all { it is String }) { "JSON 对象字段必须是字符串" }
                append('{')
                value.keys.map { it as String }.sorted().forEachIndexed { i, key ->
                    if (i > 0) append(',')
                    write(key); append(':'); write(value[key])
                }
                append('}')
            }
            is List<*> -> { append('['); value.forEachIndexed { i, item -> if (i > 0) append(','); write(item) }; append(']') }
            else -> throw IllegalArgumentException("备份包含不支持的数据类型")
        }
    }

    fun parse(text: String): Any? = Parser(text).parse()

    private class Parser(private val text: String) {
        private var index = 0
        private var nodes = 0
        fun parse(): Any? {
            val result = value(0)
            whitespace()
            require(index == text.length) { "备份 JSON 尾部存在多余内容" }
            return result
        }
        private fun whitespace() { while (index < text.length && text[index] in " \t\r\n") index++ }
        private fun value(depth: Int): Any? {
            require(depth < 20 && ++nodes <= 4_000_000) { "备份结构过深或数据过多" }
            whitespace()
            require(index < text.length) { "备份 JSON 不完整" }
            return when (text[index]) {
                '{' -> {
                    index++; whitespace()
                    val result = linkedMapOf<String, Any?>()
                    if (take('}')) result else {
                        do {
                            whitespace(); require(index < text.length && text[index] == '"') { "备份对象字段格式错误" }
                            val key = string()
                            require(!result.containsKey(key)) { "备份包含重复字段 $key" }
                            whitespace(); require(take(':')) { "备份字段缺少冒号" }
                            result[key] = value(depth + 1)
                            whitespace()
                        } while (take(','))
                        require(take('}')) { "备份对象未闭合" }; result
                    }
                }
                '[' -> {
                    index++; whitespace()
                    val result = mutableListOf<Any?>()
                    if (take(']')) result else {
                        do { result.add(value(depth + 1)); whitespace() } while (take(','))
                        require(take(']')) { "备份数组未闭合" }; result
                    }
                }
                '"' -> string()
                'n' -> literal("null", null)
                't' -> literal("true", true)
                'f' -> literal("false", false)
                '-', in '0'..'9' -> number()
                else -> throw IllegalArgumentException("请选择完整的 JSON 备份，CSV 不能用于恢复")
            }
        }
        private fun <T> literal(word: String, value: T): T {
            require(text.startsWith(word, index)) { "备份 JSON 值无效" }; index += word.length; return value
        }
        private fun take(c: Char): Boolean = if (text.getOrNull(index) == c) { index++; true } else false
        private fun number(): Long {
            val start = index
            take('-')
            require(text.getOrNull(index) in '0'..'9') { "备份数字格式错误" }
            if (!take('0')) while (text.getOrNull(index) in '0'..'9') index++
            require(text.getOrNull(index) !in listOf('.', 'e', 'E') && text.getOrNull(index) !in '0'..'9') { "备份仅允许整数，不能包含小数或指数" }
            return text.substring(start, index).toLongOrNull() ?: throw IllegalArgumentException("备份整数超出范围")
        }
        private fun string(): String {
            require(take('"'))
            val result = StringBuilder()
            while (index < text.length) {
                val c = text[index++]
                if (c == '"') {
                    val str = result.toString()
                    var offset = 0
                    while (offset < str.length) {
                        val ch = str[offset++]
                        if (Character.isHighSurrogate(ch)) { require(offset < str.length && Character.isLowSurrogate(str[offset])) { "备份含无效 Unicode" }; offset++ }
                        else require(!Character.isLowSurrogate(ch)) { "备份含无效 Unicode" }
                    }
                    return str
                }
                require(c >= ' ') { "备份字符串包含未转义控制字符" }
                if (c != '\\') result.append(c) else {
                    require(index < text.length) { "备份字符串不完整" }
                    when (val escaped = text[index++]) {
                        '"', '\\', '/' -> result.append(escaped)
                        'b' -> result.append('\b')
                        'f' -> result.append('\u000c')
                        'n' -> result.append('\n')
                        'r' -> result.append('\r')
                        't' -> result.append('\t')
                        'u' -> {
                            require(index + 4 <= text.length) { "备份 Unicode 转义不完整" }
                            val code = text.substring(index, index + 4).toIntOrNull(16) ?: throw IllegalArgumentException("备份 Unicode 转义错误")
                            result.append(code.toChar()); index += 4
                        }
                        else -> throw IllegalArgumentException("备份字符串转义错误")
                    }
                }
            }
            throw IllegalArgumentException("备份字符串未闭合")
        }
    }
}
