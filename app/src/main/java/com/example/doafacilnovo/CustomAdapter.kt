import android.content.Context
import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.example.doafacilnovo.InformacoesDoacao
import com.example.doafacilnovo.R

class CustomAdapter(context: Context, private val listaDoacao: ArrayList<InformacoesDoacao>) : BaseAdapter() {
    private val inflater: LayoutInflater = LayoutInflater.from(context)

    override fun getCount(): Int = listaDoacao.size
    override fun getItem(position: Int): Any = listaDoacao[position]
    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: inflater.inflate(R.layout.item_lista, parent, false)

        val doacao = listaDoacao[position]
        view.findViewById<TextView>(R.id.titulo).text = doacao.titulo
        view.findViewById<TextView>(R.id.nome).text = doacao.nome
        val statusDisponibilidadeTextView = view.findViewById<TextView>(R.id.statusDisponibilidade)
        statusDisponibilidadeTextView.text = doacao.disponibilidade

        // Define a cor do status
        val color = if (doacao.disponibilidade == "indisponível") R.color.vermelho else R.color.verde
        statusDisponibilidadeTextView.setTextColor(ContextCompat.getColor(view.context, color))

        // Converte URL para Bitmap usando Glide
        val imageView = view.findViewById<ImageView>(R.id.myImageViewDoacao)
        if (!doacao.primeiraImagem.isNullOrEmpty()) {
            Glide.with(view.context)
                .asBitmap() // Converte a URL diretamente para Bitmap
                .load(doacao.primeiraImagem)
                .placeholder(R.drawable.ic_adicionar)
                .error(R.drawable.ic_adicionar)
                .into(imageView)
        } else {
            imageView.setImageResource(R.drawable.ic_adicionar)
        }

        return view
    }



    private class ViewHolder(view: View) {
        val titulo: TextView = view.findViewById(R.id.titulo)
        val nome: TextView = view.findViewById(R.id.nome)
        val statusDisponibilidade: TextView = view.findViewById(R.id.statusDisponibilidade)
        val imagem: ImageView = view.findViewById(R.id.myImageViewDoacao)
    }
}


