package it.coderit.banktestapp.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import it.coderit.banktestapp.model.Movimento;
import it.coderit.banktestapp.model.TipoCentro;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MovimentoRepository implements PanacheRepository<Movimento> {

    public List<Movimento> findByCentroTipo(TipoCentro tipoCentro) {
        // 'tipoCentro' è il nome del campo direttamente nella classe Movimento
        return list("tipoCentro = ?1", tipoCentro);
    }

    public Optional<Movimento> findByTransactionId(String transactionId) {
    return getEntityManager()
        .createQuery("FROM Movimento m WHERE m.transactionId = :id", Movimento.class)
        .setParameter("id", transactionId)
        .getResultStream()
        .findFirst();
}
    public List<Movimento> findByAccountIdAndDateRange(String accountId, OffsetDateTime fromDate, OffsetDateTime toDate) {
        return list("accountId = ?1 AND bookingDate >= ?2 AND bookingDate <= ?3", accountId, fromDate, toDate);
    }

}
