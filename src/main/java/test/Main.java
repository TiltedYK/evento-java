package test;

import model.*;
import service.*;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Scanner;

public class Main {
    private static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        PartnershipRequestService prs = new PartnershipRequestService();
        CollaborationService cols = new CollaborationService();
        ReferralHitService rhs = new ReferralHitService();
        EventService es = new EventService();
        ReservationService ress = new ReservationService();
        CategoryService cats = new CategoryService();
        ProductService prods = new ProductService();
        PostService posts = new PostService();
        CommentService coms = new CommentService();
        UserService us = new UserService();

        boolean running = true;
        while (running) {
            System.out.println("\n=================================");
            System.out.println("       MENU INTERACTIF");
            System.out.println("=================================");
            System.out.println("1)  Gérer les Demandes de Partenariat");
            System.out.println("2)  Gérer les Collaborations");
            System.out.println("3)  Gérer les Referral Hits");
            System.out.println("4)  Gérer les Événements");
            System.out.println("5)  Gérer les Réservations");
            System.out.println("6)  Gérer les Catégories (Shop)");
            System.out.println("7)  Gérer les Produits (Shop)");
            System.out.println("8)  Gérer les Posts (Forum)");
            System.out.println("9)  Gérer les Commentaires (Forum)");
            System.out.println("10) Gérer les Utilisateurs");
            System.out.println("0)  Quitter");
            System.out.print("Veuillez choisir une option : ");

            String choixStr = scanner.nextLine();
            int choix = -1;
            try {
                choix = Integer.parseInt(choixStr);
            } catch (NumberFormatException e) {
                System.out.println("Choix invalide.");
                continue;
            }

            switch (choix) {
                case 1: menuPartnership(prs); break;
                case 2: menuCollaboration(cols); break;
                case 3: menuReferralHit(rhs); break;
                case 4: menuEvent(es); break;
                case 5: menuReservation(ress); break;
                case 6: menuCategory(cats); break;
                case 7: menuProduct(prods); break;
                case 8: menuPost(posts); break;
                case 9: menuComment(coms); break;
                case 10: menuUser(us); break;
                case 0:
                    running = false;
                    System.out.println("Au revoir !");
                    break;
                default:
                    System.out.println("Choix introuvable, réessayez.");
            }
        }
    }

    // ==========================================
    // 1) PARTNERSHIP REQUEST
    // ==========================================
    private static void menuPartnership(PartnershipRequestService prs) {
        boolean back = false;
        while (!back) {
            System.out.println("\n--- MENU PARTNERSHIP REQUEST ---");
            System.out.println("1) Ajouter une demande");
            System.out.println("2) Afficher toutes les demandes");
            System.out.println("3) Modifier une demande");
            System.out.println("4) Supprimer une demande");
            System.out.println("5) Rechercher par nom (Stream)");
            System.out.println("6) Rechercher par status (Stream)");
            System.out.println("7) Rechercher par entreprise (Stream)");
            System.out.println("0) Retour au menu principal");
            System.out.print("Choix : ");
            String choix = scanner.nextLine();

            try {
                switch (choix) {
                    case "1":
                        System.out.print("Nom du contact : ");
                        String nom = scanner.nextLine();
                        System.out.print("Email : ");
                        String email = scanner.nextLine();
                        System.out.print("Entreprise : ");
                        String company = scanner.nextLine();
                        System.out.print("Téléphone : ");
                        String phone = scanner.nextLine();
                        System.out.print("Message : ");
                        String message = scanner.nextLine();
                        prs.ajouter(new PartnershipRequest(nom, email, company, phone, message));
                        System.out.println("=> Demande ajoutée avec succès !");
                        break;
                    case "2":
                        System.out.println("Liste des demandes :");
                        prs.recuperer().forEach(System.out::println);
                        break;
                    case "3":
                        System.out.print("ID à modifier : ");
                        int mid = Integer.parseInt(scanner.nextLine());
                        System.out.print("Nouveau nom : ");
                        String newNom = scanner.nextLine();
                        System.out.print("Nouvel email : ");
                        String newEmail = scanner.nextLine();
                        System.out.print("Nouvelle entreprise : ");
                        String newComp = scanner.nextLine();
                        System.out.print("Nouveau téléphone : ");
                        String newPhone = scanner.nextLine();
                        System.out.print("Nouveau message : ");
                        String newMsg = scanner.nextLine();
                        System.out.print("Nouveau status (pending/confirmed/rejected) : ");
                        String newStatus = scanner.nextLine();
                        PartnershipRequest pr = new PartnershipRequest();
                        pr.setId(mid);
                        pr.setContactName(newNom);
                        pr.setEmail(newEmail);
                        pr.setCompanyName(newComp);
                        pr.setPhone(newPhone);
                        pr.setMessage(newMsg);
                        pr.setStatus(newStatus);
                        prs.modifier(pr);
                        System.out.println("=> Demande modifiée avec succès !");
                        break;
                    case "4":
                        System.out.print("Entrez l'ID à supprimer : ");
                        int id = Integer.parseInt(scanner.nextLine());
                        prs.supprimer(id);
                        System.out.println("=> Demande " + id + " supprimée avec succès !");
                        break;
                    case "5":
                        System.out.print("Nom à rechercher : ");
                        prs.rechercherParNom(scanner.nextLine()).forEach(System.out::println);
                        break;
                    case "6":
                        System.out.print("Status à rechercher : ");
                        prs.rechercherParStatus(scanner.nextLine()).forEach(System.out::println);
                        break;
                    case "7":
                        System.out.print("Entreprise à rechercher : ");
                        prs.rechercherParCompany(scanner.nextLine()).forEach(System.out::println);
                        break;
                    case "0": back = true; break;
                    default: System.out.println("Choix invalide.");
                }
            } catch (Exception e) {
                System.out.println("Erreur : " + e.getMessage());
            }
        }
    }

    // ==========================================
    // 2) COLLABORATION
    // ==========================================
    private static void menuCollaboration(CollaborationService cs) {
        boolean back = false;
        while (!back) {
            System.out.println("\n--- MENU COLLABORATION ---");
            System.out.println("1) Ajouter une collaboration");
            System.out.println("2) Afficher toutes les collaborations");
            System.out.println("3) Modifier une collaboration");
            System.out.println("4) Supprimer une collaboration");
            System.out.println("5) Rechercher par titre (Stream)");
            System.out.println("6) Rechercher par status (Stream)");
            System.out.println("7) Rechercher par type (Stream)");
            System.out.println("0) Retour au menu principal");
            System.out.print("Choix : ");
            String choix = scanner.nextLine();

            try {
                switch (choix) {
                    case "1":
                        System.out.print("Titre : ");
                        String titre = scanner.nextLine();
                        System.out.print("Type (image/video) : ");
                        String type = scanner.nextLine();
                        System.out.print("ID du partenaire existant : ");
                        int pId = Integer.parseInt(scanner.nextLine());
                        Collaboration c = new Collaboration(pId, titre, type, "default.png", "https://link.com", "top", LocalDate.now(), LocalDate.now().plusMonths(1));
                        cs.ajouter(c);
                        System.out.println("=> Collaboration ajoutée avec succès !");
                        break;
                    case "2":
                        System.out.println("Liste des collaborations :");
                        cs.recuperer().forEach(System.out::println);
                        break;
                    case "3":
                        System.out.print("ID à modifier : ");
                        int cid = Integer.parseInt(scanner.nextLine());
                        System.out.print("Nouveau titre : ");
                        String newT = scanner.nextLine();
                        System.out.print("Nouveau type (image/video) : ");
                        String newType = scanner.nextLine();
                        System.out.print("Nouveau status (pending/approved/rejected) : ");
                        String newSt = scanner.nextLine();
                        System.out.print("Position (top/bottom) : ");
                        String newPos = scanner.nextLine();
                        System.out.print("Partner ID : ");
                        int newPid = Integer.parseInt(scanner.nextLine());
                        Collaboration cu = new Collaboration();
                        cu.setId(cid);
                        cu.setTitle(newT);
                        cu.setType(newType);
                        cu.setStatus(newSt);
                        cu.setPosition(newPos);
                        cu.setPartnerId(newPid);
                        cu.setFileName("default.png");
                        cs.modifier(cu);
                        System.out.println("=> Collaboration modifiée avec succès !");
                        break;
                    case "4":
                        System.out.print("Entrez l'ID à supprimer : ");
                        int id = Integer.parseInt(scanner.nextLine());
                        cs.supprimer(id);
                        System.out.println("=> Collaboration " + id + " supprimée avec succès !");
                        break;
                    case "5":
                        System.out.print("Titre à rechercher : ");
                        cs.rechercherParTitre(scanner.nextLine()).forEach(System.out::println);
                        break;
                    case "6":
                        System.out.print("Status à rechercher : ");
                        cs.rechercherParStatus(scanner.nextLine()).forEach(System.out::println);
                        break;
                    case "7":
                        System.out.print("Type à rechercher : ");
                        cs.rechercherParType(scanner.nextLine()).forEach(System.out::println);
                        break;
                    case "0": back = true; break;
                    default: System.out.println("Choix invalide.");
                }
            } catch (Exception e) {
                System.out.println("Erreur : " + e.getMessage());
            }
        }
    }

    // ==========================================
    // 3) REFERRAL HIT
    // ==========================================
    private static void menuReferralHit(ReferralHitService rhs) {
        boolean back = false;
        while (!back) {
            System.out.println("\n--- MENU REFERRAL HIT ---");
            System.out.println("1) Ajouter un Referral Hit");
            System.out.println("2) Afficher tous");
            System.out.println("3) Supprimer");
            System.out.println("0) Retour au menu principal");
            System.out.print("Choix : ");
            String choix = scanner.nextLine();

            try {
                switch (choix) {
                    case "1":
                        System.out.print("Influencer ID : ");
                        int infId = Integer.parseInt(scanner.nextLine());
                        System.out.print("Session ID : ");
                        String sessId = scanner.nextLine();
                        System.out.print("Referred User ID (0 si vide) : ");
                        int refUserId = Integer.parseInt(scanner.nextLine());
                        Integer rId = (refUserId == 0) ? null : refUserId;
                        ReferralHit rh = new ReferralHit(infId, sessId, rId);
                        rhs.ajouter(rh);
                        System.out.println("=> Referral Hit ajouté avec succès !");
                        break;
                    case "2":
                        System.out.println("Liste des Referral Hits :");
                        rhs.recuperer().forEach(System.out::println);
                        break;
                    case "3":
                        System.out.print("Entrez l'ID à supprimer : ");
                        int id = Integer.parseInt(scanner.nextLine());
                        rhs.supprimer(id);
                        System.out.println("=> Referral Hit " + id + " supprimé avec succès !");
                        break;
                    case "0": back = true; break;
                    default: System.out.println("Choix invalide.");
                }
            } catch (Exception e) {
                System.out.println("Erreur : " + e.getMessage());
            }
        }
    }

    // ==========================================
    // 4) ÉVÉNEMENT
    // ==========================================
    private static void menuEvent(EventService es) {
        boolean back = false;
        while (!back) {
            System.out.println("\n--- MENU ÉVÉNEMENT ---");
            System.out.println("1) Ajouter un événement");
            System.out.println("2) Afficher tous les événements");
            System.out.println("3) Modifier un événement");
            System.out.println("4) Supprimer un événement");
            System.out.println("5) Rechercher par titre (Stream)");
            System.out.println("6) Rechercher par statut (Stream)");
            System.out.println("7) Rechercher par venue (Stream)");
            System.out.println("0) Retour au menu principal");
            System.out.print("Choix : ");
            String choix = scanner.nextLine();

            try {
                switch (choix) {
                    case "1":
                        System.out.print("Titre : ");
                        String titre = scanner.nextLine();
                        System.out.print("Date (AAAA-MM-JJ) : ");
                        String dateStr = scanner.nextLine();
                        System.out.print("Heure (HH:MM) : ");
                        String heureStr = scanner.nextLine();
                        LocalDateTime dt = LocalDateTime.parse(dateStr + "T" + heureStr);
                        System.out.print("Capacité : ");
                        int cap = Integer.parseInt(scanner.nextLine());
                        System.out.print("Description : ");
                        String desc = scanner.nextLine();
                        System.out.print("Statut (active/cancelled/completed) : ");
                        String statut = scanner.nextLine();
                        System.out.print("Venue : ");
                        String venue = scanner.nextLine();
                        Event ev = new Event(titre, dt, cap, desc, statut, venue);
                        System.out.print("Location (optionnel, Entrée pour passer) : ");
                        String loc = scanner.nextLine();
                        if (!loc.isEmpty()) ev.setLocation(loc);
                        System.out.print("Genre (optionnel, Entrée pour passer) : ");
                        String genre = scanner.nextLine();
                        if (!genre.isEmpty()) ev.setGenre(genre);
                        es.ajouter(ev);
                        System.out.println("=> Événement ajouté avec succès !");
                        break;
                    case "2":
                        System.out.println("Liste des événements :");
                        es.recuperer().forEach(System.out::println);
                        break;
                    case "3":
                        System.out.print("ID à modifier : ");
                        int eid = Integer.parseInt(scanner.nextLine());
                        System.out.print("Nouveau titre : ");
                        String nt = scanner.nextLine();
                        System.out.print("Date (AAAA-MM-JJ) : ");
                        String nd = scanner.nextLine();
                        System.out.print("Heure (HH:MM) : ");
                        String nh = scanner.nextLine();
                        LocalDateTime ndt = LocalDateTime.parse(nd + "T" + nh);
                        System.out.print("Capacité : ");
                        int nc = Integer.parseInt(scanner.nextLine());
                        System.out.print("Description : ");
                        String ndesc = scanner.nextLine();
                        System.out.print("Statut : ");
                        String nst = scanner.nextLine();
                        System.out.print("Venue : ");
                        String nv = scanner.nextLine();
                        Event eu = new Event(eid, nt, ndt, nc, ndesc, nst, nv);
                        System.out.print("Location (optionnel) : ");
                        String nloc = scanner.nextLine();
                        if (!nloc.isEmpty()) eu.setLocation(nloc);
                        System.out.print("Genre (optionnel) : ");
                        String ng = scanner.nextLine();
                        if (!ng.isEmpty()) eu.setGenre(ng);
                        es.modifier(eu);
                        System.out.println("=> Événement modifié avec succès !");
                        break;
                    case "4":
                        System.out.print("Entrez l'ID à supprimer : ");
                        int did = Integer.parseInt(scanner.nextLine());
                        es.supprimer(did);
                        System.out.println("=> Événement " + did + " supprimé avec succès !");
                        break;
                    case "5":
                        System.out.print("Titre à rechercher : ");
                        es.rechercherParTitre(scanner.nextLine()).forEach(System.out::println);
                        break;
                    case "6":
                        System.out.print("Statut à rechercher : ");
                        es.rechercherParStatut(scanner.nextLine()).forEach(System.out::println);
                        break;
                    case "7":
                        System.out.print("Venue à rechercher : ");
                        es.rechercherParVenue(scanner.nextLine()).forEach(System.out::println);
                        break;
                    case "0": back = true; break;
                    default: System.out.println("Choix invalide.");
                }
            } catch (Exception e) {
                System.out.println("Erreur : " + e.getMessage());
            }
        }
    }

    // ==========================================
    // 5) RÉSERVATION
    // ==========================================
    private static void menuReservation(ReservationService rs) {
        boolean back = false;
        while (!back) {
            System.out.println("\n--- MENU RÉSERVATION ---");
            System.out.println("1) Ajouter une réservation");
            System.out.println("2) Afficher toutes les réservations");
            System.out.println("3) Modifier une réservation");
            System.out.println("4) Supprimer une réservation");
            System.out.println("5) Rechercher par statut (Stream)");
            System.out.println("6) Rechercher par event_id (Stream)");
            System.out.println("7) Rechercher par user_id (Stream)");
            System.out.println("0) Retour au menu principal");
            System.out.print("Choix : ");
            String choix = scanner.nextLine();

            try {
                switch (choix) {
                    case "1":
                        System.out.print("Event ID : ");
                        int evId = Integer.parseInt(scanner.nextLine());
                        System.out.print("User ID : ");
                        int uId = Integer.parseInt(scanner.nextLine());
                        System.out.print("Nombre de places : ");
                        int nb = Integer.parseInt(scanner.nextLine());
                        rs.ajouter(new Reservation(evId, uId, nb));
                        System.out.println("=> Réservation ajoutée avec succès !");
                        break;
                    case "2":
                        System.out.println("Liste des réservations :");
                        rs.recuperer().forEach(System.out::println);
                        break;
                    case "3":
                        System.out.print("ID à modifier : ");
                        int rid = Integer.parseInt(scanner.nextLine());
                        System.out.print("Event ID : ");
                        int nevId = Integer.parseInt(scanner.nextLine());
                        System.out.print("User ID : ");
                        int nuId = Integer.parseInt(scanner.nextLine());
                        System.out.print("Nombre de places : ");
                        int nnb = Integer.parseInt(scanner.nextLine());
                        System.out.print("Statut (pending/confirmed/cancelled) : ");
                        String nstat = scanner.nextLine();
                        Reservation ru = new Reservation();
                        ru.setId(rid);
                        ru.setEventId(nevId);
                        ru.setUserId(nuId);
                        ru.setNombrePlaces(nnb);
                        ru.setStatut(nstat);
                        rs.modifier(ru);
                        System.out.println("=> Réservation modifiée avec succès !");
                        break;
                    case "4":
                        System.out.print("Entrez l'ID à supprimer : ");
                        int did = Integer.parseInt(scanner.nextLine());
                        rs.supprimer(did);
                        System.out.println("=> Réservation " + did + " supprimée avec succès !");
                        break;
                    case "5":
                        System.out.print("Statut à rechercher : ");
                        rs.rechercherParStatut(scanner.nextLine()).forEach(System.out::println);
                        break;
                    case "6":
                        System.out.print("Event ID à rechercher : ");
                        rs.rechercherParEvent(Integer.parseInt(scanner.nextLine())).forEach(System.out::println);
                        break;
                    case "7":
                        System.out.print("User ID à rechercher : ");
                        rs.rechercherParUser(Integer.parseInt(scanner.nextLine())).forEach(System.out::println);
                        break;
                    case "0": back = true; break;
                    default: System.out.println("Choix invalide.");
                }
            } catch (Exception e) {
                System.out.println("Erreur : " + e.getMessage());
            }
        }
    }

    // ==========================================
    // 6) CATÉGORIE (SHOP)
    // ==========================================
    private static void menuCategory(CategoryService cs) {
        boolean back = false;
        while (!back) {
            System.out.println("\n--- MENU CATÉGORIE ---");
            System.out.println("1) Ajouter une catégorie");
            System.out.println("2) Afficher toutes les catégories");
            System.out.println("3) Modifier une catégorie");
            System.out.println("4) Supprimer une catégorie");
            System.out.println("5) Rechercher par nom (Stream)");
            System.out.println("0) Retour au menu principal");
            System.out.print("Choix : ");
            String choix = scanner.nextLine();

            try {
                switch (choix) {
                    case "1":
                        System.out.print("Nom : ");
                        String name = scanner.nextLine();
                        System.out.print("Description : ");
                        String desc = scanner.nextLine();
                        cs.ajouter(new Category(name, desc));
                        System.out.println("=> Catégorie ajoutée avec succès !");
                        break;
                    case "2":
                        System.out.println("Liste des catégories :");
                        cs.recuperer().forEach(System.out::println);
                        break;
                    case "3":
                        System.out.print("ID à modifier : ");
                        int cid = Integer.parseInt(scanner.nextLine());
                        System.out.print("Nouveau nom : ");
                        String nn = scanner.nextLine();
                        System.out.print("Nouvelle description : ");
                        String nd = scanner.nextLine();
                        cs.modifier(new Category(cid, nn, nd));
                        System.out.println("=> Catégorie modifiée avec succès !");
                        break;
                    case "4":
                        System.out.print("Entrez l'ID à supprimer : ");
                        int did = Integer.parseInt(scanner.nextLine());
                        cs.supprimer(did);
                        System.out.println("=> Catégorie " + did + " supprimée avec succès !");
                        break;
                    case "5":
                        System.out.print("Nom à rechercher : ");
                        cs.rechercherParNom(scanner.nextLine()).forEach(System.out::println);
                        break;
                    case "0": back = true; break;
                    default: System.out.println("Choix invalide.");
                }
            } catch (Exception e) {
                System.out.println("Erreur : " + e.getMessage());
            }
        }
    }

    // ==========================================
    // 7) PRODUIT (SHOP)
    // ==========================================
    private static void menuProduct(ProductService ps) {
        boolean back = false;
        while (!back) {
            System.out.println("\n--- MENU PRODUIT ---");
            System.out.println("1) Ajouter un produit");
            System.out.println("2) Afficher tous les produits");
            System.out.println("3) Modifier un produit");
            System.out.println("4) Supprimer un produit");
            System.out.println("5) Rechercher par nom (Stream)");
            System.out.println("6) Rechercher par artiste (Stream)");
            System.out.println("7) Rechercher disponibles (Stream)");
            System.out.println("8) Rechercher par catégorie (Stream)");
            System.out.println("0) Retour au menu principal");
            System.out.print("Choix : ");
            String choix = scanner.nextLine();

            try {
                switch (choix) {
                    case "1":
                        System.out.print("Nom : ");
                        String name = scanner.nextLine();
                        System.out.print("Description : ");
                        String desc = scanner.nextLine();
                        System.out.print("Prix : ");
                        double prix = Double.parseDouble(scanner.nextLine());
                        System.out.print("Stock : ");
                        int stock = Integer.parseInt(scanner.nextLine());
                        System.out.print("Nom de l'artiste : ");
                        String artist = scanner.nextLine();
                        System.out.print("Category ID : ");
                        int catId = Integer.parseInt(scanner.nextLine());
                        ps.ajouter(new Product(name, desc, prix, stock, artist, catId));
                        System.out.println("=> Produit ajouté avec succès !");
                        break;
                    case "2":
                        System.out.println("Liste des produits :");
                        ps.recuperer().forEach(System.out::println);
                        break;
                    case "3":
                        System.out.print("ID à modifier : ");
                        int pid = Integer.parseInt(scanner.nextLine());
                        System.out.print("Nom : ");
                        String nn = scanner.nextLine();
                        System.out.print("Description : ");
                        String nd = scanner.nextLine();
                        System.out.print("Prix : ");
                        double np = Double.parseDouble(scanner.nextLine());
                        System.out.print("Stock : ");
                        int ns = Integer.parseInt(scanner.nextLine());
                        System.out.print("Artiste : ");
                        String na = scanner.nextLine();
                        System.out.print("Category ID : ");
                        int nci = Integer.parseInt(scanner.nextLine());
                        Product pu = new Product();
                        pu.setId(pid);
                        pu.setName(nn);
                        pu.setDescription(nd);
                        pu.setPrice(np);
                        pu.setStock(ns);
                        pu.setArtistName(na);
                        pu.setCategoryId(nci);
                        pu.setAvailable(true);
                        ps.modifier(pu);
                        System.out.println("=> Produit modifié avec succès !");
                        break;
                    case "4":
                        System.out.print("Entrez l'ID à supprimer : ");
                        int did = Integer.parseInt(scanner.nextLine());
                        ps.supprimer(did);
                        System.out.println("=> Produit " + did + " supprimé avec succès !");
                        break;
                    case "5":
                        System.out.print("Nom à rechercher : ");
                        ps.rechercherParNom(scanner.nextLine()).forEach(System.out::println);
                        break;
                    case "6":
                        System.out.print("Artiste à rechercher : ");
                        ps.rechercherParArtiste(scanner.nextLine()).forEach(System.out::println);
                        break;
                    case "7":
                        System.out.println("Produits disponibles :");
                        ps.rechercherDisponibles().forEach(System.out::println);
                        break;
                    case "8":
                        System.out.print("Category ID : ");
                        ps.rechercherParCategorie(Integer.parseInt(scanner.nextLine())).forEach(System.out::println);
                        break;
                    case "0": back = true; break;
                    default: System.out.println("Choix invalide.");
                }
            } catch (Exception e) {
                System.out.println("Erreur : " + e.getMessage());
            }
        }
    }

    // ==========================================
    // 8) POST (FORUM)
    // ==========================================
    private static void menuPost(PostService ps) {
        boolean back = false;
        while (!back) {
            System.out.println("\n--- MENU POST ---");
            System.out.println("1) Ajouter un post");
            System.out.println("2) Afficher tous les posts");
            System.out.println("3) Modifier un post");
            System.out.println("4) Supprimer un post");
            System.out.println("5) Rechercher par titre (Stream)");
            System.out.println("6) Rechercher par contenu (Stream)");
            System.out.println("7) Rechercher par auteur (Stream)");
            System.out.println("0) Retour au menu principal");
            System.out.print("Choix : ");
            String choix = scanner.nextLine();

            try {
                switch (choix) {
                    case "1":
                        System.out.print("Author ID : ");
                        int aid = Integer.parseInt(scanner.nextLine());
                        System.out.print("Titre : ");
                        String titre = scanner.nextLine();
                        System.out.print("Contenu : ");
                        String contenu = scanner.nextLine();
                        ps.ajouter(new Post(aid, titre, contenu));
                        System.out.println("=> Post ajouté avec succès !");
                        break;
                    case "2":
                        System.out.println("Liste des posts :");
                        ps.recuperer().forEach(System.out::println);
                        break;
                    case "3":
                        System.out.print("ID à modifier : ");
                        int pid = Integer.parseInt(scanner.nextLine());
                        System.out.print("Nouveau titre : ");
                        String nt = scanner.nextLine();
                        System.out.print("Nouveau contenu : ");
                        String nc = scanner.nextLine();
                        Post postU = new Post();
                        postU.setId(pid);
                        postU.setTitle(nt);
                        postU.setSlug(nt.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", ""));
                        postU.setContent(nc);
                        ps.modifier(postU);
                        System.out.println("=> Post modifié avec succès !");
                        break;
                    case "4":
                        System.out.print("Entrez l'ID à supprimer : ");
                        int did = Integer.parseInt(scanner.nextLine());
                        ps.supprimer(did);
                        System.out.println("=> Post " + did + " supprimé avec succès !");
                        break;
                    case "5":
                        System.out.print("Titre à rechercher : ");
                        ps.rechercherParTitre(scanner.nextLine()).forEach(System.out::println);
                        break;
                    case "6":
                        System.out.print("Contenu à rechercher : ");
                        ps.rechercherParContenu(scanner.nextLine()).forEach(System.out::println);
                        break;
                    case "7":
                        System.out.print("Author ID : ");
                        ps.rechercherParAuteur(Integer.parseInt(scanner.nextLine())).forEach(System.out::println);
                        break;
                    case "0": back = true; break;
                    default: System.out.println("Choix invalide.");
                }
            } catch (Exception e) {
                System.out.println("Erreur : " + e.getMessage());
            }
        }
    }

    // ==========================================
    // 9) COMMENTAIRE (FORUM)
    // ==========================================
    private static void menuComment(CommentService coms) {
        boolean back = false;
        while (!back) {
            System.out.println("\n--- MENU COMMENTAIRE ---");
            System.out.println("1) Ajouter un commentaire");
            System.out.println("2) Afficher tous les commentaires");
            System.out.println("3) Modifier un commentaire");
            System.out.println("4) Supprimer un commentaire");
            System.out.println("5) Rechercher par contenu (Stream)");
            System.out.println("6) Rechercher par post_id (Stream)");
            System.out.println("7) Rechercher par auteur (Stream)");
            System.out.println("0) Retour au menu principal");
            System.out.print("Choix : ");
            String choix = scanner.nextLine();

            try {
                switch (choix) {
                    case "1":
                        System.out.print("Post ID : ");
                        int postId = Integer.parseInt(scanner.nextLine());
                        System.out.print("Author ID : ");
                        int authId = Integer.parseInt(scanner.nextLine());
                        System.out.print("Contenu : ");
                        String contenu = scanner.nextLine();
                        coms.ajouter(new Comment(postId, authId, contenu));
                        System.out.println("=> Commentaire ajouté avec succès !");
                        break;
                    case "2":
                        System.out.println("Liste des commentaires :");
                        coms.recuperer().forEach(System.out::println);
                        break;
                    case "3":
                        System.out.print("ID à modifier : ");
                        int cid = Integer.parseInt(scanner.nextLine());
                        System.out.print("Nouveau contenu : ");
                        String nc = scanner.nextLine();
                        Comment comU = new Comment();
                        comU.setId(cid);
                        comU.setContent(nc);
                        coms.modifier(comU);
                        System.out.println("=> Commentaire modifié avec succès !");
                        break;
                    case "4":
                        System.out.print("Entrez l'ID à supprimer : ");
                        int did = Integer.parseInt(scanner.nextLine());
                        coms.supprimer(did);
                        System.out.println("=> Commentaire " + did + " supprimé avec succès !");
                        break;
                    case "5":
                        System.out.print("Contenu à rechercher : ");
                        coms.rechercherParContenu(scanner.nextLine()).forEach(System.out::println);
                        break;
                    case "6":
                        System.out.print("Post ID : ");
                        coms.rechercherParPost(Integer.parseInt(scanner.nextLine())).forEach(System.out::println);
                        break;
                    case "7":
                        System.out.print("Author ID : ");
                        coms.rechercherParAuteur(Integer.parseInt(scanner.nextLine())).forEach(System.out::println);
                        break;
                    case "0": back = true; break;
                    default: System.out.println("Choix invalide.");
                }
            } catch (Exception e) {
                System.out.println("Erreur : " + e.getMessage());
            }
        }
    }

    // ==========================================
    // 10) UTILISATEUR
    // ==========================================
    private static void menuUser(UserService us) {
        boolean back = false;
        while (!back) {
            System.out.println("\n--- MENU UTILISATEUR ---");
            System.out.println("1) Ajouter un utilisateur");
            System.out.println("2) Afficher tous les utilisateurs");
            System.out.println("3) Modifier un utilisateur");
            System.out.println("4) Supprimer un utilisateur");
            System.out.println("5) Rechercher par nom (Stream)");
            System.out.println("6) Rechercher par email (Stream)");
            System.out.println("7) Rechercher par localisation (Stream)");
            System.out.println("0) Retour au menu principal");
            System.out.print("Choix : ");
            String choix = scanner.nextLine();

            try {
                switch (choix) {
                    case "1":
                        System.out.print("Nom : ");
                        String nom = scanner.nextLine();
                        System.out.print("Prénom : ");
                        String prenom = scanner.nextLine();
                        System.out.print("Email : ");
                        String email = scanner.nextLine();
                        System.out.print("Téléphone (8 chiffres) : ");
                        String tel = scanner.nextLine();
                        System.out.print("Mot de passe : ");
                        String pwd = scanner.nextLine();
                        User u = new User(nom, prenom, email, tel, pwd);
                        System.out.print("Date de naissance (AAAA-MM-JJ, Entrée pour passer) : ");
                        String dn = scanner.nextLine();
                        if (!dn.isEmpty()) u.setDateNaissance(LocalDate.parse(dn));
                        System.out.print("Localisation (optionnel, Entrée pour passer) : ");
                        String loc = scanner.nextLine();
                        if (!loc.isEmpty()) u.setLocalisation(loc);
                        us.ajouter(u);
                        System.out.println("=> Utilisateur ajouté avec succès !");
                        break;
                    case "2":
                        System.out.println("Liste des utilisateurs :");
                        us.recuperer().forEach(System.out::println);
                        break;
                    case "3":
                        System.out.print("ID à modifier : ");
                        int uid = Integer.parseInt(scanner.nextLine());
                        System.out.print("Nouveau nom : ");
                        String nn = scanner.nextLine();
                        System.out.print("Nouveau prénom : ");
                        String np = scanner.nextLine();
                        System.out.print("Nouvel email : ");
                        String ne = scanner.nextLine();
                        System.out.print("Nouveau téléphone : ");
                        String nt = scanner.nextLine();
                        System.out.print("Date de naissance (AAAA-MM-JJ, Entrée pour passer) : ");
                        String ndn = scanner.nextLine();
                        System.out.print("Localisation (optionnel) : ");
                        String nloc = scanner.nextLine();
                        User uu = new User();
                        uu.setId(uid);
                        uu.setNom(nn);
                        uu.setPrenom(np);
                        uu.setEmail(ne);
                        uu.setNumTelephone(nt);
                        if (!ndn.isEmpty()) uu.setDateNaissance(LocalDate.parse(ndn));
                        if (!nloc.isEmpty()) uu.setLocalisation(nloc);
                        us.modifier(uu);
                        System.out.println("=> Utilisateur modifié avec succès !");
                        break;
                    case "4":
                        System.out.print("Entrez l'ID à supprimer : ");
                        int did = Integer.parseInt(scanner.nextLine());
                        us.supprimer(did);
                        System.out.println("=> Utilisateur " + did + " supprimé avec succès !");
                        break;
                    case "5":
                        System.out.print("Nom à rechercher : ");
                        us.rechercherParNom(scanner.nextLine()).forEach(System.out::println);
                        break;
                    case "6":
                        System.out.print("Email à rechercher : ");
                        us.rechercherParEmail(scanner.nextLine()).forEach(System.out::println);
                        break;
                    case "7":
                        System.out.print("Localisation à rechercher : ");
                        us.rechercherParLocalisation(scanner.nextLine()).forEach(System.out::println);
                        break;
                    case "0": back = true; break;
                    default: System.out.println("Choix invalide.");
                }
            } catch (Exception e) {
                System.out.println("Erreur : " + e.getMessage());
            }
        }
    }
}
