CREATE TABLE Caregivers (
    Username varchar(255),
    Salt BINARY(16),
    Hash BINARY(16),
    PRIMARY KEY (Username)
);

CREATE TABLE Availabilities (
    Time date,
    Username varchar(255) REFERENCES Caregivers,
    PRIMARY KEY (Time, Username)
);

CREATE TABLE Vaccines (
    Name varchar(255),
    Doses int,
    PRIMARY KEY (Name)
);

CREATE TABLE Patients (
                          Username varchar(255),
                          Salt BINARY(16),
                          Hash BINARY(16),
                          PRIMARY KEY (Username)
);

CREATE TABLE Appointments (
                              appoID INT,
                              Time Date,
                              Patient varchar(255) REFERENCES Patients,
                              Caregiver varchar(255) REFERENCES Caregivers,
                              Vaccine varchar(255) REFERENCES Vaccines,
                              PRIMARY KEY (appoID)
);