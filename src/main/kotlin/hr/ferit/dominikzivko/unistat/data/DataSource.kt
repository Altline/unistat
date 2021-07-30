package hr.ferit.dominikzivko.unistat.data

import hr.ferit.dominikzivko.unistat.AppComponent
import hr.ferit.dominikzivko.unistat.gui.component.ProgressMonitor
import java.util.*

interface DataSource : AppComponent {
    val userID: UUID?
    fun fetchGeneralData(progressMonitor: ProgressMonitor? = null): User
    fun fetchBills(existingBills: List<Bill>, progressMonitor: ProgressMonitor? = null): List<Bill>

    /**
     * Revokes any and all rights to data access that a user might have through this data source, requiring fresh
     * authentication upon a next fetch.
     */
    fun revokeAuthorization()
}