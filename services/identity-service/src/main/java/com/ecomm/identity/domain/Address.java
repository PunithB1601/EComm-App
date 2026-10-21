package com.ecomm.identity.domain;

import jakarta.persistence.*;

@Entity
@Table(name="addresses")
public class Address {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    public Long getId() { return id; }
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="user_id", nullable=false)
    private User user;

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    @Column(name="recipient_name", nullable=false, length=200)
    private String recipientName;

    public String getRecipientName() { return recipientName; }
    public void setRecipientName(String recipientName) { this.recipientName = recipientName; }
    @Column(name="phone", nullable=false, length=32)
    private String phone;

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    @Column(name="line1", nullable=false, length=255)
    private String line1;

    public String getLine1() { return line1; }
    public void setLine1(String line1) { this.line1 = line1; }
    @Column(name="line2", nullable=true, length=255)
    private String line2;

    public String getLine2() { return line2; }
    public void setLine2(String line2) { this.line2 = line2; }
    @Column(name="city", nullable=false, length=100)
    private String city;

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    @Column(name="state", nullable=false, length=100)
    private String state;

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    @Column(name="postal_code", nullable=false, length=20)
    private String postalCode;

    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }
    @Column(name="country_code", nullable=false, length=2)
    private String countryCode;

    public String getCountryCode() { return countryCode; }
    public void setCountryCode(String countryCode) { this.countryCode = countryCode; }
}
