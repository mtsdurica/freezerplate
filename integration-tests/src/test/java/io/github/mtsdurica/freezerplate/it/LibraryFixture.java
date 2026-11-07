package io.github.mtsdurica.freezerplate.it;

import io.github.mtsdurica.freezerplate.it.model.Author;
import io.github.mtsdurica.freezerplate.it.model.Book;
import io.github.mtsdurica.freezerplate.it.model.Review;
import io.github.mtsdurica.freezerplate.it.repository.AuthorRepository;
import io.github.mtsdurica.freezerplate.it.repository.BookRepository;
import io.github.mtsdurica.freezerplate.it.repository.ReviewRepository;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class LibraryFixture {

  private final AuthorRepository authorRepository;
  private final BookRepository bookRepository;
  private final ReviewRepository reviewRepository;

  private Author herbert;
  private Author tolkien;
  private Author pratchett;
  private Author gaiman;

  private Book dune;
  private Book hobbit;
  private Book lordOfTheRings;
  private Book goodOmens;

  private Review hobbitPraise;
  private Review hobbitCritique;

  public LibraryFixture(
      AuthorRepository authorRepository,
      BookRepository bookRepository,
      ReviewRepository reviewRepository) {
    this.authorRepository = authorRepository;
    this.bookRepository = bookRepository;
    this.reviewRepository = reviewRepository;
  }

  public void reset() {
    clear();
    createAuthors();
    createBooks();
    createReviews();
  }

  private void clear() {
    reviewRepository.deleteAll();
    bookRepository.deleteAll();
    authorRepository.deleteAll();
  }

  private void createAuthors() {
    herbert = authorRepository.save(new Author("Frank Herbert"));
    tolkien = authorRepository.save(new Author("J.R.R. Tolkien"));
    pratchett = authorRepository.save(new Author("Terry Pratchett"));
    gaiman = authorRepository.save(new Author("Neil Gaiman"));
  }

  private void createBooks() {
    dune = bookRepository.save(new Book("Dune", Set.of(herbert)));
    hobbit = bookRepository.save(new Book("The Hobbit", Set.of(tolkien)));
    lordOfTheRings = bookRepository.save(new Book("The Lord of the Rings", Set.of(tolkien)));
    goodOmens = bookRepository.save(new Book("Good Omens", Set.of(pratchett, gaiman)));
  }

  private void createReviews() {
    hobbitPraise = reviewRepository.save(new Review(5, "A timeless adventure", hobbit));
    hobbitCritique =
        reviewRepository.save(new Review(4, "Charming, a bit slow in the middle", hobbit));
  }

  public Author herbert() {
    return herbert;
  }

  public Author tolkien() {
    return tolkien;
  }

  public Author pratchett() {
    return pratchett;
  }

  public Author gaiman() {
    return gaiman;
  }

  public Book dune() {
    return dune;
  }

  public Book hobbit() {
    return hobbit;
  }

  public Book lordOfTheRings() {
    return lordOfTheRings;
  }

  public Book goodOmens() {
    return goodOmens;
  }

  public Review hobbitPraise() {
    return hobbitPraise;
  }

  public Review hobbitCritique() {
    return hobbitCritique;
  }
}
