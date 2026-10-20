import { apiClient } from './client'
import { cleanParams } from './customersApi'

/** Axios serialises arrays as status[]=..; Spring expects status=A&status=B. */
const paramsSerializer = { indexes: null }

export const fetchLeads = async (params) => (await apiClient.get('/leads', { params: cleanParams(params), paramsSerializer })).data
export const fetchLeadBoard = async (params) => (await apiClient.get('/leads/board', { params: cleanParams(params) })).data
export const fetchLeadSummary = async (params) => (await apiClient.get('/leads/summary', { params: cleanParams(params) })).data
export const fetchLead = async (id) => (await apiClient.get(`/leads/${id}`)).data
export const createLead = async (body) => (await apiClient.post('/leads', body)).data
export const updateLead = async (id, body) => (await apiClient.put(`/leads/${id}`, body)).data
export const changeLeadStatus = async (id, body) => (await apiClient.patch(`/leads/${id}/status`, body)).data
export const assignLead = async (id, officerId) => (await apiClient.patch(`/leads/${id}/assign`, { officerId })).data
/** Returns { lead, sale, alreadyConverted }. Safe to repeat: the backend never creates a second sale. */
export const convertLead = async (id, amount) => (await apiClient.post(`/leads/${id}/convert`, { amount })).data

export const fetchSales = async (status) => (await apiClient.get('/sales', { params: status ? { status } : {} })).data
export const decideSale = async (id, decision, note) => (await apiClient.patch(`/sales/${id}/decision`, { decision, note })).data
