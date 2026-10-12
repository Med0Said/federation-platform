// ═══════════════════════════════════════════════════════════════════════════
// Domain models — mirror the backend response DTOs
// ═══════════════════════════════════════════════════════════════════════════

import type { UserRole, UserStatus } from './index';

export type Gender = 'MALE' | 'FEMALE' | 'OTHER';
export type AthleteCategory = 'YOUTH' | 'JUNIOR' | 'SENIOR' | 'MASTERS' | 'GRAND_MASTERS';

export type CompetitionStatus =
  | 'DRAFT' | 'PUBLISHED' | 'REGISTRATION_OPEN' | 'REGISTRATION_CLOSED'
  | 'ONGOING' | 'COMPLETED' | 'CANCELLED';

export type RegistrationStatus = 'PENDING' | 'CONFIRMED' | 'WAITLISTED' | 'CANCELLED' | 'DISQUALIFIED';
export type ResultStatus = 'UNOFFICIAL' | 'OFFICIAL' | 'PROTESTED' | 'DISQUALIFIED' | 'DNS' | 'DNF' | 'DSQ';
export type NewsStatus = 'DRAFT' | 'REVIEW' | 'PUBLISHED' | 'ARCHIVED';
export type NewsCategory = 'GENERAL' | 'COMPETITION' | 'ATHLETE' | 'CLUB' | 'ANNOUNCEMENT' | 'PRESS_RELEASE';

export interface Club {
  id:             string;
  name:           string;
  shortName?:     string;
  slug:           string;
  licenseNumber:  string;
  city?:          string;
  region?:        string;
  country:        string;
  address?:       string;
  foundedYear?:   number;
  description?:   string;
  logoUrl?:       string;
  website?:       string;
  email?:         string;
  phone?:         string;
  status:         'ACTIVE' | 'SUSPENDED' | 'DISSOLVED';
  managerId?:     string;
  managerName?:   string;
  activeAthletes: number;
  createdAt:      string;
}

export interface AthleteDetail {
  id:                     string;
  licenseNumber:          string;
  licenseExpiry?:         string;
  firstName:              string;
  lastName:               string;
  fullName:               string;
  dateOfBirth:            string;
  age:                    number;
  gender:                 Gender;
  nationality:            string;
  countryCode:            string;
  category?:              AthleteCategory;
  status:                 string;
  weightKg?:              number;
  heightCm?:              number;
  clubId?:                string;
  clubName?:              string;
  email?:                 string;
  phone?:                 string;
  emergencyContactName?:  string;
  emergencyContactPhone?: string;
  medicalClearanceDate?:  string;
  medicalNotes?:          string;
  photoUrl?:              string;
  notes?:                 string;
  userId?:                string;
  /** Contact, medical and internal fields were withheld from the current user. */
  restricted:             boolean;
}

export interface Competition {
  id:                     string;
  name:                   string;
  slug:                   string;
  edition?:               string;
  description?:           string;
  sport:                  string;
  level:                  'LOCAL' | 'REGIONAL' | 'NATIONAL' | 'INTERNATIONAL';
  format:                 'INDIVIDUAL' | 'TEAM' | 'RELAY' | 'MIXED';
  status:                 CompetitionStatus;
  venueName?:             string;
  venueCity?:             string;
  venueCountry?:          string;
  startDate:              string;
  endDate:                string;
  registrationDeadline?:  string;
  maxParticipants?:       number;
  maxPerClub?:            number;
  hostClubId?:            string;
  hostClubName?:          string;
  organizerName?:         string;
  rulesUrl?:              string;
  posterUrl?:             string;
  prizeInfo?:             string;
  entryFee?:              number;
  currency?:              string;
  eventCount:             number;
  confirmedRegistrations: number;
  publishedAt?:           string;
}

export interface CompetitionEvent {
  id:                string;
  competitionId:     string;
  competitionName:   string;
  name:              string;
  code?:             string;
  discipline:        string;
  genderCategory?:   Gender;
  ageCategory?:      AthleteCategory;
  scheduledAt?:      string;
  durationMinutes?:  number;
  maxParticipants?:  number;
  scoringUnit?:      string;
  lowerIsBetter:     boolean;
  status:            CompetitionStatus;
  notes?:            string;
  registrationCount: number;
  waitlistCount:     number;
}

export interface Registration {
  id:                    string;
  registrationNumber:    string;
  competitionId:         string;
  competitionName:       string;
  competitionStartDate?: string;
  competitionStatus?:    CompetitionStatus;
  eventId:               string;
  eventName:             string;
  discipline:            string;
  athleteId:             string;
  athleteName:           string;
  licenseNumber:         string;
  clubId?:               string;
  clubName?:             string;
  registeredByName?:     string;
  bibNumber?:            number;
  seedValue?:            number;
  seedUnit?:             string;
  status:                RegistrationStatus;
  waitlistPosition?:     number;
  confirmedAt?:          string;
  cancelledAt?:          string;
  cancellationReason?:   string;
  feeAmount?:            number;
  feeCurrency?:          string;
  feePaid:               boolean;
  medicalWaiver:         boolean;
  notes?:                string;
  createdAt:             string;
}

export interface Result {
  id:                string;
  competitionId:     string;
  competitionName:   string;
  eventId:           string;
  eventName:         string;
  discipline:        string;
  athleteId:         string;
  athleteName:       string;
  licenseNumber:     string;
  gender?:           Gender;
  category?:         string;
  clubId?:           string;
  clubName?:         string;
  round:             string;
  heatNumber?:       number;
  laneNumber?:       number;
  performanceValue?: number;
  performanceUnit?:  string;
  performanceText?:  string;
  windSpeed?:        number;
  points?:           number;
  status:            ResultStatus;
  personalBest:      boolean;
  seasonBest:        boolean;
  competitionRecord: boolean;
  nationalRecord:    boolean;
  rankPosition?:     number;
  medal?:            'GOLD' | 'SILVER' | 'BRONZE';
  notes?:            string;
  createdAt:         string;
}

export interface Ranking {
  id:                string;
  eventId:           string;
  eventName:         string;
  round:             string;
  rankPosition:      number;
  medal?:            'GOLD' | 'SILVER' | 'BRONZE';
  resultId:          string;
  athleteId:         string;
  athleteName:       string;
  licenseNumber:     string;
  clubId?:           string;
  clubName?:         string;
  performanceValue?: number;
  performanceText?:  string;
  points?:           number;
  official:          boolean;
}

export interface Tag {
  id:     string;
  name:   string;
  slug:   string;
  color?: string;
}

export interface NewsArticle {
  id:                      string;
  title:                   string;
  slug:                    string;
  excerpt?:                string;
  content:                 string;
  category:                NewsCategory;
  status:                  NewsStatus;
  language:                string;
  coverUrl?:               string;
  coverAltText?:           string;
  authorId?:               string;
  authorName?:             string;
  relatedCompetitionId?:   string;
  relatedCompetitionName?: string;
  relatedAthleteId?:       string;
  relatedClubId?:          string;
  featured:                boolean;
  pinned:                  boolean;
  viewCount:               number;
  publishedAt?:            string;
  createdAt:               string;
  tags?:                   Tag[];
}

export interface ManagedUser {
  id:                 string;
  email:              string;
  username:           string;
  firstName:          string;
  lastName:           string;
  fullName:           string;
  role:               UserRole;
  status:             UserStatus;
  phone?:             string;
  avatarUrl?:         string;
  lastLogin?:         string;
  createdAt:          string;
  athleteId?:         string;
  temporaryPassword?: string;
}

export interface DashboardStats {
  totalAthletes:           number;
  activeAthletes:          number;
  totalClubs:              number;
  activeClubs:             number;
  totalCompetitions:       number;
  upcomingCompetitions:    number;
  openForRegistration:     number;
  pendingRegistrations:    number;
  confirmedRegistrations:  number;
  waitlistedRegistrations: number;
  publishedNews:           number;
  totalUsers:              number;
}

// ── Display helpers ─────────────────────────────────────────────────────────

export const COMPETITION_STATUSES: CompetitionStatus[] = [
  'DRAFT', 'PUBLISHED', 'REGISTRATION_OPEN', 'REGISTRATION_CLOSED', 'ONGOING', 'COMPLETED', 'CANCELLED',
];

export const ATHLETE_CATEGORIES: { value: AthleteCategory; label: string }[] = [
  { value: 'YOUTH',         label: 'Youth (U18)' },
  { value: 'JUNIOR',        label: 'Junior (18–20)' },
  { value: 'SENIOR',        label: 'Senior (21–34)' },
  { value: 'MASTERS',       label: 'Masters (35–49)' },
  { value: 'GRAND_MASTERS', label: 'Grand Masters (50+)' },
];

export const NEWS_CATEGORIES: NewsCategory[] = [
  'GENERAL', 'COMPETITION', 'ATHLETE', 'CLUB', 'ANNOUNCEMENT', 'PRESS_RELEASE',
];

export const RESULT_STATUSES: ResultStatus[] = ['UNOFFICIAL', 'OFFICIAL', 'PROTESTED', 'DNS', 'DNF', 'DSQ'];

/** "REGISTRATION_OPEN" → "Registration open" */
export function humanize(value: string | null | undefined): string {
  if (!value) return '';
  if (/^[A-Z]{2,3}$/.test(value)) return value; // DNS, DNF, DSQ…
  const s = value.replace(/^ROLE_/, '').replace(/_/g, ' ').toLowerCase();
  return s.charAt(0).toUpperCase() + s.slice(1);
}
