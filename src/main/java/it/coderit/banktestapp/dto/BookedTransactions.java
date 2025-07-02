package it.coderit.banktestapp.dto;

import java.util.List;

import it.coderit.banktestapp.model.Transaction;
//Serve per fare il mapping del JSON che ha questa struttura.
public class BookedTransactions {
    //booked corrisponde al nome del nodo JSON che contiene la lista di movimenti.
    //La lista usa la entity Transaction.
    private List<Transaction> booked;

    public List<Transaction> getBooked() {
        return booked;
    }

    public void setBooked(List<Transaction> booked) {
        this.booked = booked;
    }

}


