import com.google.firebase.Timestamp

data class FechamentoFatura(
    val dataInicio: Timestamp = Timestamp.now(),
    val dataFechamento: Timestamp = Timestamp.now(),
    val criadoEm: Timestamp = Timestamp.now()
)
