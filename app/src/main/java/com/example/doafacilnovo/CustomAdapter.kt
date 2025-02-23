import android.content.Context
import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.doafacilnovo.InformacoesDoacao
import com.example.doafacilnovo.R

class CustomAdapter(context: Context, private val listaDoacao: ArrayList<InformacoesDoacao>) : BaseAdapter() {
    private val inflater: LayoutInflater = LayoutInflater.from(context)

    override fun getCount(): Int = listaDoacao.size

    override fun getItem(position: Int): Any = listaDoacao[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        // Usando a View reciclada ou criando uma nova
        val view = convertView ?: inflater.inflate(R.layout.item_lista, parent, false)

        // Obtendo o item de doação da lista
        val doacao = listaDoacao[position]

        // Atualizando os TextViews para mostrar o Título e o Nome do Usuário
        view.findViewById<TextView>(R.id.titulo).text = doacao.titulo
        view.findViewById<TextView>(R.id.nome).text = doacao.nome
        val statusDisponibilidadeTextView = view.findViewById<TextView>(R.id.statusDisponibilidade)
        statusDisponibilidadeTextView.text = doacao.disponibilidade

        // Alterando a cor do texto de disponibilidade
        if (doacao.disponibilidade == "indisponível") {
            statusDisponibilidadeTextView.setTextColor(ContextCompat.getColor(view.context, R.color.vermelho))
        } else {
            statusDisponibilidadeTextView.setTextColor(ContextCompat.getColor(view.context, R.color.verde))
        }

        // Atualiza o ImageView com a primeira imagem (caso exista)
        val imageView = view.findViewById<ImageView>(R.id.myImageViewDoacao)
        if (doacao.primeiraImagem != null) {
            imageView.setImageBitmap(doacao.primeiraImagem) // Aqui você usa o Bitmap para exibir a imagem
        } else {
            imageView.setImageResource(R.drawable.ic_adicionar) // Imagem padrão caso não exista
        }

        // Retorna a view preenchida
        return view
    }
}
