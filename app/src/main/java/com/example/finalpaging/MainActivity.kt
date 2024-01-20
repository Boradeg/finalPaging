package com.example.finalpaging
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import android.widget.ProgressBar
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.paging.PagingSource
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.paging.LoadStateAdapter
import androidx.paging.PagingState
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.flow.Flow
import okhttp3.OkHttpClient
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

data class DataClass (
    val patientListDto: List<PatientListDto>,
    val pageEventDto: PageEventDto
)

data class PageEventDto (
    val pageNumber: Long,
    val totalPages: Long,
    val totalRows: Long,
    val rowsPerPage: Long
)

data class PatientListDto (
    val clinicName: ClinicName,
    val lName: String,
    val patientID: Long,
    val inviteStatus: Long,
    val stateID: Long,
    val mobile: String,
    val cityID: Long,
    val userName: String,
    val countryID: Long,
    val doctorName: DoctorName,
    val patientChartNo: String,
    val fName: String,
    val primaryClinicID: Long,
    val email: String,
    val primaryDoctorID: Long,
    val lastVisitedDate: String? = null
)

enum class ClinicName {
    AddisonHealthClinic,
    ChicagoClinic,
    Clinic2
}

enum class DoctorName {
    ChandlerDoc,
    FredRick,
    PriyaDoc
}


interface QuotableApi {

    @GET("api/Patient/{clinicId}/{stateId}/{countryId}/{pageNumber}/10")
    suspend fun getQuotes(
        @Header("Authorization") token: String,
        @Path("clinicId") clinicId: Int,
        @Path("stateId") stateId: Int,
        @Path("countryId") countryId: Int,
        @Path("pageNumber") pageNumber: Int
    ): DataClass
    @GET("api/SearchPatient/{clinicId}/{searchText}/{pageNumber}/10")
    suspend fun searchPatients(
        @Header("Authorization") token: String,
        @Path("clinicId") clinicId: Int,
        @Path("searchText") searchText: String,
        @Path("pageNumber") pageNumber: Int
    ): DataClass
}

class QuotesPagingSource(private val quotableApi: QuotableApi) : PagingSource<Int, PatientListDto>() {

    private val token = "Bearer eyJhbGciOiJSUzI1NiIsImtpZCI6ImQwVm5wVG9wQU9jN05fUDFDQjVkb1EiLCJ0eXAiOiJhdCtqd3QifQ.eyJuYmYiOjE3MDU3MzI0NjQsImV4cCI6MTcwNTczNjA2NCwiaXNzIjoiaHR0cHM6Ly92aXRhaW5zaWdodHMtaWRlbnRpdHkuYXp1cmV3ZWJzaXRlcy5uZXQiLCJhdWQiOlsiZGdfYXBwb2ludG1lbnRfYXBpIiwiSWRlbnRpdHlTZXJ2ZXJBcGkiLCJ0ZXN0X3NlcnZpY2UiXSwiY2xpZW50X2lkIjoiVml0YUFuZ3VsYXJXZWJUZXN0Iiwic3ViIjoiNzM5ODliYjctOTk4NC00MGEyLWE1OWEtZjE2OTFkOTdmODgxIiwiYXV0aF90aW1lIjoxNzA1NzMyNDY0LCJpZHAiOiJsb2NhbCIsInJvbGUiOiJzdXBlcnVzZXIiLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJjY2hpY2FnbyIsIm5hbWUiOiJDY2hpY2FnbyBDbGluaWMiLCJlbWFpbCI6ImNoaWNhZ28uY2xpZW50QHlvcG1haWwuY29tIiwicGhvbmVOdW1iZXIiOiI0NTY0NTU0OTc5IiwiZmlyc3ROYW1lIjoiQ2NoaWNhZ28iLCJsYXN0TmFtZSI6IkNsaW5pYyIsImNsaWVudElkIjoiMzM4IiwiY2xpbmljSWQiOiI0MTMiLCJwcm92aWRlcklkIjoiNzQ4IiwiYXBwVXNlcklkIjoiMCIsInNjb3BlIjpbImVtYWlsIiwib3BlbmlkIiwicHJvZmlsZSIsInJvbGUiLCJ0ZW5lbnQiLCJkZ19hcHBvaW50bWVudF9hcGkiLCJJZGVudGl0eVNlcnZlckFwaSIsInRlc3Rfc2VydmljZSJdLCJhbXIiOlsicHdkIl19.Mylk6UNdpyWYmU6ruguMsNhOX-wLL5GxkaBAX8y4xsBZM3CkigCUvKggimcdUNp0ZuhgjZw1VvCJH6H2KSqHf2YuZ1LsESXg8NxM9DmELUV4D62x865TcFr-UdyvHkrFeP2y56RpbSJY0dgXem1zMGwgGoWvAKbqmdY8S4cavvSQFwQTy_lcuJgqVhrXNwVDP3_zQZ1d93_fIrOwGog7xezCTxCEwd4H1bWue9bffJFWt-80MC6bWEBsgxuu_73U3ZDAo66wgX1gPQ5FlO3sRDzbdAUnTG_uKOOPAeNxRxp1rFLbArbdlxBFXoS4AKWAC8eUeXK8riDx0N4Pg_Yg2w"

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, PatientListDto> {
        try {
            val page = params.key ?: 1
            val response = quotableApi.getQuotes(token, 338, 0, 0, page)
            val quotes = response.patientListDto

            // Return results with next key (page) for paging
            return LoadResult.Page(
                data = quotes,
                prevKey = if (page == 1) null else page - 1,
                nextKey = if (quotes.isEmpty()) null else page + 1
            )
        } catch (e: Exception) {
            // Handle error
            return LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, PatientListDto>): Int? {
        // Not needed for this example
        return null
    }
}

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var quotesAdapter: QuotesAdapter
    private lateinit var progressBar: ProgressBar
    private val BASE_URL = "https://vitainsights-api.azurewebsites.net/"
    private  val TOKEN = "Bearer eyJhbGciOiJSUzI1NiIsImtpZCI6ImQwVm5wVG9wQU9jN05fUDFDQjVkb1EiLCJ0eXAiOiJhdCtqd3QifQ.eyJuYmYiOjE3MDU3NDM3NTAsImV4cCI6MTcwNTc0NzM1MCwiaXNzIjoiaHR0cHM6Ly92aXRhaW5zaWdodHMtaWRlbnRpdHkuYXp1cmV3ZWJzaXRlcy5uZXQiLCJhdWQiOlsiZGdfYXBwb2ludG1lbnRfYXBpIiwiSWRlbnRpdHlTZXJ2ZXJBcGkiLCJ0ZXN0X3NlcnZpY2UiXSwiY2xpZW50X2lkIjoiVml0YUFuZ3VsYXJXZWJUZXN0Iiwic3ViIjoiNzM5ODliYjctOTk4NC00MGEyLWE1OWEtZjE2OTFkOTdmODgxIiwiYXV0aF90aW1lIjoxNzA1NzQzNzUwLCJpZHAiOiJsb2NhbCIsInJvbGUiOiJzdXBlcnVzZXIiLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJjY2hpY2FnbyIsIm5hbWUiOiJDY2hpY2FnbyBDbGluaWMiLCJlbWFpbCI6ImNoaWNhZ28uY2xpZW50QHlvcG1haWwuY29tIiwicGhvbmVOdW1iZXIiOiI0NTY0NTU0OTc5IiwiZmlyc3ROYW1lIjoiQ2NoaWNhZ28iLCJsYXN0TmFtZSI6IkNsaW5pYyIsImNsaWVudElkIjoiMzM4IiwiY2xpbmljSWQiOiI0MTMiLCJwcm92aWRlcklkIjoiNzQ4IiwiYXBwVXNlcklkIjoiMCIsInNjb3BlIjpbImVtYWlsIiwib3BlbmlkIiwicHJvZmlsZSIsInJvbGUiLCJ0ZW5lbnQiLCJkZ19hcHBvaW50bWVudF9hcGkiLCJJZGVudGl0eVNlcnZlckFwaSIsInRlc3Rfc2VydmljZSJdLCJhbXIiOlsicHdkIl19.uurqDdVB39hCIp3zMyFgNA4awMx4KRRZS73DtGNChQJn9zL3AiONe1J10tnErCOKt9071pQ3WmYDAxcqH16mUs65ZHeXrYdVqPtuaXiy6XFDz-b05tOwqE7ZZkmrUgJQKzBGg27HutXmZMMEb9DzZkzmPWD_3MfCAg4LW04dc_fZVN_P8V1_DwSJkfcGkjm0Sp3SgJfkIEi4qieZJ9g7GaCXpcTAve8AFbpnd5MX0GbgKgRbVnoO5J3Zr8HJdFPqjXsqwbn8OG4tgFJf0A3-SDryAvnvO4P6mY80bwjccyVFwESaS-VBrSZ2t_oAeqc3DNxfro2dT3yJSM-fkQFlDg"
    val okHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val original = chain.request()
            val request = original.newBuilder()
                .header("Authorization", TOKEN)
                .method(original.method(), original.body())
                .build()
            chain.proceed(request)
        }
        .build()

    private val quotableApi = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .client(okHttpClient)
        .build()
        .create(QuotableApi::class.java)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recyclerView)
        progressBar = findViewById(R.id.progressBar)

        quotesAdapter = QuotesAdapter()

        val layoutManager = LinearLayoutManager(this)
        recyclerView.layoutManager = layoutManager

        // Attach the load state adapter
        recyclerView.adapter = quotesAdapter.withLoadStateHeaderAndFooter(
            header = QuotesLoadStateAdapter { quotesAdapter.retry() },
            footer = QuotesLoadStateAdapter { quotesAdapter.retry() }
        )

        val pager = Pager(PagingConfig(pageSize = 1)) {
            QuotesPagingSource(quotableApi)
        }

        val pagingDataFlow = pager.flow

        lifecycleScope.launch {
            pagingDataFlow.collectLatest { pagingData ->
                quotesAdapter.submitData(pagingData)
            }
        }
    }
}
class QuotesLoadStateAdapter(private val retry: () -> Unit) : LoadStateAdapter<QuotesLoadStateAdapter.LoadStateViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, loadState: LoadState): LoadStateViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_loading, parent, false)
        return LoadStateViewHolder(view, retry)
    }

    override fun onBindViewHolder(holder: LoadStateViewHolder, loadState: LoadState) {
        holder.bind(loadState)
    }

    class LoadStateViewHolder(itemView: View, retry: () -> Unit) : RecyclerView.ViewHolder(itemView) {
        private val progressBar: ProgressBar = itemView.findViewById(R.id.loadStateProgressBar)
        private val textView: TextView = itemView.findViewById(R.id.loadStateTextView)
        private val retryButton: Button = itemView.findViewById(R.id.retryButton)

        init {
            retryButton.setOnClickListener { retry.invoke() }
        }

        fun bind(loadState: LoadState) {
            when (loadState) {
                is LoadState.Loading -> {
                    progressBar.visibility = View.VISIBLE
                    textView.text = "Loading..."
                    retryButton.visibility = View.GONE
                }
                is LoadState.Error -> {
                    progressBar.visibility = View.GONE
                    textView.text = "Error: ${loadState.error.localizedMessage}"
                    retryButton.visibility = View.VISIBLE
                }
                is LoadState.NotLoading -> {
                    progressBar.visibility = View.GONE
                    textView.text = ""
                    retryButton.visibility = View.GONE
                }
            }
        }
    }
}

class QuotesAdapter :
    PagingDataAdapter<PatientListDto, RecyclerView.ViewHolder>(QUOTES_COMPARATOR) {

    private val VIEW_TYPE_QUOTE = 0
    private val VIEW_TYPE_PROGRESS = 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_QUOTE -> {
                val view =
                    LayoutInflater.from(parent.context).inflate(R.layout.item_quote, parent, false)
                QuoteViewHolder(view)
            }
            VIEW_TYPE_PROGRESS -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_loading, parent, false)
                ProgressViewHolder(view)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is QuoteViewHolder -> getItem(position)?.let { holder.bind(it) }
            is ProgressViewHolder -> holder.bind()
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (position < itemCount && getItem(position) != null) {
            VIEW_TYPE_QUOTE
        } else {
            VIEW_TYPE_PROGRESS
        }
    }

    class QuoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val authorTextView: TextView = itemView.findViewById(R.id.authorTextView)
        private val contentTextView: TextView = itemView.findViewById(R.id.contentTextView)

        fun bind(quote: PatientListDto) {
            authorTextView.text = quote.userName
            contentTextView.text = quote.mobile
        }
    }

    class ProgressViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val progressBar: ProgressBar = itemView.findViewById(R.id.progressBar)

        fun bind() {
            // You can customize the progress bar as needed
            progressBar.visibility = View.VISIBLE
        }
    }

    companion object {
        private val QUOTES_COMPARATOR = object : DiffUtil.ItemCallback<PatientListDto>() {
            override fun areItemsTheSame(oldItem: PatientListDto, newItem: PatientListDto): Boolean {
                return oldItem.patientID == newItem.patientID
            }

            override fun areContentsTheSame(oldItem: PatientListDto, newItem: PatientListDto): Boolean {
                return oldItem == newItem
            }
        }
    }
}
