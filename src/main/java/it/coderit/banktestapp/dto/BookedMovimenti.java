package it.coderit.banktestapp.dto;

import java.util.List;

import it.coderit.banktestapp.model.Movimento;
//Serve per fare il mapping del JSON che ha questa struttura.
public class BookedMovimenti {
    //booked corrisponde al nome del nodo JSON che contiene la lista di movimenti.
    //La lista usa la entity Movimento.
    private List<Movimento> booked;

    public List<Movimento> getBooked() {
        return booked;
    }

    public void setBooked(List<Movimento> booked) {
        this.booked = booked;
    }

}


