package it.coderit.banktestapp.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import it.coderit.banktestapp.model.Transaction;
import it.coderit.banktestapp.model.CenterType;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TransactionRepository implements PanacheRepository<Transaction> {

    public List<Transaction> findByCentroTipo(CenterType centerType) {
        // 'centerType' è il nome del campo direttamente nella classe Transaction
        return list("centerType = ?1", centerType);
    }

    public Optional<Transaction> findByTransactionId(String transactionId) {
    return getEntityManager()
        .createQuery("FROM Transaction m WHERE m.transactionId = :id", Transaction.class)
        .setParameter("id", transactionId)
        .getResultStream()
        .findFirst();
}
    public List<Transaction> findByAccountIdAndDateRange(String accountId, OffsetDateTime fromDate, OffsetDateTime toDate) {
        return list("accountId = ?1 AND bookingDate >= ?2 AND bookingDate <= ?3", accountId, fromDate, toDate);
    }

}
